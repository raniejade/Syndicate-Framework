package org.q2.syndicate;

import org.q2.bluetooth.BtConnection;
import org.q2.bluetooth.BtConnectionNotifier;
import org.q2.bluetooth.BtConnector;
import org.q2.util.DebugLog;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.UUID;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.ConcurrentHashMap;

import static org.q2.syndicate.SyndicateProperties.*;
import static org.q2.util.DebugLog.Log;
import static org.q2.util.MD5.md5;

final class SyndicateCore {
    private static SyndicateCore ourInstance;

    static {
        try {
            ourInstance = new SyndicateCore();
        } catch (BluetoothStateException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public final UUID SYNDICATE_UUID;

    public static SyndicateCore getInstance() {
        return ourInstance;
    }

    // indicates whether Syndicate will use L2CAP as its main communication protocol
    private boolean useL2CAP;

    private volatile LocalDevice localDevice;

    private final ReentrantReadWriteLock mutex;

    private final HashMap<String, ConnectionHandler> connections;

    private ServerThread serverThread;

    private ClientThread clientThread;

    private final RoutingTable routingTable;

    private final ReentrantLock lock;

    private volatile boolean master;

    private final ConcurrentLinkedQueue<Packet> in;

    private final ConcurrentHashMap<String, String> history;

    private SyndicateCore() throws BluetoothStateException {
        // initialize properties
        initProperties();
        Log("SynCore", "properties initialized");
        SYNDICATE_UUID = new UUID("04A6C7B", false);
        localDevice = LocalDevice.getLocalDevice();
        Log("SynCore", "local device initialized");

        mutex = new ReentrantReadWriteLock();
        connections = new HashMap<String, ConnectionHandler>();
        routingTable = new RoutingTable();
        routingTable.add(localDevice.getBluetoothAddress());
        lock = new ReentrantLock();
        master = false;
        in = new ConcurrentLinkedQueue<Packet>();
	history = new ConcurrentHashMap<String, String>();
    }

    public synchronized void setMaster(boolean t) {
        master = t;
    }

    public synchronized boolean isMaster() {
        return master;
    }

    private void runCoreThreads() {
        serverThread.start();
        clientThread.start();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    public void initialize() {
        serverThread = new ServerThread();
        clientThread = new ClientThread();

        runCoreThreads();
        Log("SynCore", "core threads running...");
        addShutdownHook();
        Log("SynCore", "shutdown hook added...");
        mode = 0;
    }

    private void initProperties() {
        Properties p = System.getProperties();

        String check = null;
        if ((check = p.getProperty(SYNDICATE_CONNECTION_TYPE)) != null) {
            if (check.equals(SYNDICATE_CONNECTION_RFCOMM)) {
                useL2CAP = false;
            } else {
                useL2CAP = true;
            }
        } else {
            p.setProperty(SYNDICATE_CONNECTION_TYPE, SYNDICATE_CONNECTION_L2CAP);
            useL2CAP = true;
        }

        if ((check = p.getProperty(SYNDICATE_DEBUG)) != null) {
            if (check.equals(SYNDICATE_DEBUG_TRUE)) {
                DebugLog.setEnabled(true);
            }

        } else {
            p.setProperty(SYNDICATE_DEBUG, SYNDICATE_DEBUG_FALSE);
        }
    }

    private volatile int mode;


    public void acceptConnection(BtConnection connection, boolean server) {
        mutex.writeLock().lock();
        try {
            if (!hasConnection(connection.getRemoteDevice().getBluetoothAddress()) && connections.size() < 2) {
                ConnectionHandler handler = new ConnectionHandler(connection);
                connections.put(handler.getBtAddress(), handler);
                handler.start();
                routingTable.add(localDevice.getBluetoothAddress(), handler.getBtAddress());
            } else {
                connection.close();
            }
        } catch (IOException e) {
            Log("SynCore.acceptConnection", e.getMessage());
        } finally {
            mutex.writeLock().unlock();
        }
    }

    public boolean hasConnection(String address) {
        mutex.readLock().lock();
        try {
            // check also visibility graph
            return routingTable.search(localDevice.getBluetoothAddress(), address);
        } finally {
            mutex.readLock().unlock();
        }
    }

    public BtConnection openConnection(String url) throws IOException {
        if (useL2CAP)
            return BtConnector.openL2CAPConnection(url + ";TransmitMTU=512;ReceiveMTU=512");
        return BtConnector.openRFCOMMConnection(url);
    }

    public BtConnectionNotifier openNotifier(String uuid) throws IOException {
        if (useL2CAP)
            return BtConnector.openL2CAPConnectionNotifier("btl2cap://localhost:" + uuid + ";name=Syndicate;TransmitMTU=512;ReceiveMTU=512");
        return BtConnector.openRFCOMMConnectionNotifier("btspp://localhost:" + uuid + ";name=Syndicate;");
    }

    public LocalDevice getLocalDevice() {
        return localDevice;
    }

    public Packet requestUpdatePacket() {
        mutex.readLock().lock();
        try {
            Set<String> n = connections.keySet();
            ByteBuffer buf = ByteBuffer.allocate(n.size() * 12);
            for (String i : n) {
                buf.put(i.getBytes());
            }

            return new Packet(Packet.UPDATE_PACKET, localDevice.getBluetoothAddress(),
                    "UPDATEPACKET", buf.array());
        } finally {
            mutex.readLock().unlock();
        }
    }

    private class ShutdownHook extends Thread {

        @Override
        public void run() {
            serverThread.setStop(true);
            clientThread.setStop(true);

            for (String connection : connections.keySet()) {
                ConnectionHandler handler = connections.get(connection);
                handler.setStop(true);
                try {
                    // wait for the thread to finish
                    handler.join();
                } catch (InterruptedException e) {

                }
            }

            connections.clear();
        }
    }

    public void removeConnection(String address) {
        mutex.writeLock().lock();
        try {
            if (connections.containsKey(address)) {
                ConnectionHandler handler = connections.get(address);
                connections.remove(address);
                routingTable.remove(localDevice.getBluetoothAddress(), address);
                handler.setStop(true);
            }
        } finally {
            mutex.writeLock().unlock();
        }
    }

    public void handlePacket(byte[] p, String from) {
        Packet s = Packet.createPacket(p);
        s.decreaseHopCount();
        if (s.getHopCount() > 0) {
            Log("SynCore", s.getSource() + " " + s.getDestination());
            if (s.getType() == Packet.DATA_PACKET) {
                handleDataPacket(s);
            } else {
                handleUpdatePacket(s, from);
            }
        }
    }

    public void handleUpdatePacket(Packet p, String from) {
        // dont update packets that came from this device
        if (p.getSource().equals(localDevice.getBluetoothAddress()))
            return;
	
	// get the md5 of the payload of the packet and store it
	// if the previous md5 is the same with the new one
	// then this packet is redundant
	String hash = md5(p.getPayload());
	if(history.containsKey(p.getSource())) {
	    String old = history.get(p.getSource());
	    if(old.equals(hash))
		return;
	}

        ByteBuffer buffer = ByteBuffer.allocate(p.getPayload().length);
        buffer.put(p.getPayload());
        routingTable.remove(p.getSource());
        routingTable.add(p.getSource());
        byte[] tmp = new byte[12];
        while (buffer.hasRemaining()) {
            buffer.get(tmp, 0, 12);
            routingTable.add(p.getSource(), new String(tmp));
        }

        for (String address : connections.keySet()) {
            if (!address.equals(from)) {
                connections.get(address).offer(p);
            }
        }
	history.put(p.getSource(), hash);
    }

    public void handleDataPacket(Packet p) {
        // this packet is for this device
        if (p.getDestination().equals(localDevice.getBluetoothAddress())) {
            offer(p);
        } else {
            String nextHop = routingTable.nextHop(localDevice.getBluetoothAddress(), p.getDestination());

            if (nextHop != null) {
                Log("SynCore", "Packet from: " + p.getSource() + " routed to: " + nextHop);
                for (String n : connections.keySet()) {
                    if (n.equals(nextHop)) {
                        connections.get(n).offer(p);
                    }
                }
            }
        }
    }

    public Set<String> getConnections() {
        mutex.readLock().lock();
        try {
            return routingTable.devices(localDevice.getBluetoothAddress());
        } finally {
            mutex.readLock().unlock();
        }
    }

    public synchronized void lock() {
        lock.lock();
    }

    public synchronized void unlock() {
        lock.unlock();
    }

    public synchronized ReentrantLock getLock() {
        return lock;
    }

    public synchronized void offer(Packet p) {
        in.offer(p);
    }

    public synchronized Packet poll() {
        return in.poll();
    }

    public boolean send(String destination, byte[] data) {
        mutex.writeLock().lock();
        try {
            String nextHop = routingTable.nextHop(localDevice.getBluetoothAddress(), destination);
            if (nextHop != null) {
                Packet p = new Packet(Packet.DATA_PACKET, localDevice.getBluetoothAddress(), destination, data);
                ConnectionHandler handler = connections.get(nextHop);
                if (handler != null) {
                    handler.offer(p);
                    return true;
                }
            }
        } finally {
            mutex.writeLock().unlock();
        }
        return false;
    }
}
