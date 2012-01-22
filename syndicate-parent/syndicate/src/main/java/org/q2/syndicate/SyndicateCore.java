package org.q2.syndicate;

import org.q2.bluetooth.BtConnection;
import org.q2.bluetooth.BtConnectionNotifier;
import org.q2.bluetooth.BtConnector;
import org.q2.util.DebugLog;
import org.q2.util.StateListener;
import org.q2.rip.*;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.UUID;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

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

    private final org.q2.rip.RoutingTable routes;

    private final ReentrantLock lock;

    private volatile boolean master;

    private final ConcurrentLinkedQueue<Packet> in;

    private final ConcurrentHashMap<String, String> history;

    private final ConcurrentHashMap<String, String> sent;

    private StateListener listener;

    private volatile boolean networkChanged;

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
	sent = new ConcurrentHashMap<String, String>();
	listener = null;
	networkChanged = false;

	routes = new org.q2.rip.RoutingTable();
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
	notifyListener("acceptConnection", "new connection");
        mutex.writeLock().lock();
        try {
            if (!hasConnection(connection.getRemoteDevice().getBluetoothAddress()) && connections.size() < 2) {
                ConnectionHandler handler = new ConnectionHandler(connection);
                connections.put(handler.getBtAddress(), handler);
		notifyListener("acceptConnection", "connection accepted. Starting handler...");
                handler.start();
                //routingTable.add(localDevice.getBluetoothAddress(), handler.getBtAddress());
		routes.updateEntry(new RoutingTableEntry(handler.getBtAddress(), 1, handler.getBtAddress()));
            } else {
		notifyListener("acceptConnection", "connection rejected.. reasons: link count exceeded or already visible to this network");
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
	    //routingTable.doSomethingImportant();
            return routes.contains(address);
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

    public Packet requestUpdatePacket(String destination) {
	mutex.readLock().lock();
	try {
	    Vector<org.q2.rip.RoutingTable.DestinationAdvertisement> entries = routes.getAdvertisement(destination);
	    ByteBuffer buffer = ByteBuffer.allocate(16 * entries.size() + 4);
	    buffer.putInt(entries.size());
	    for(org.q2.rip.RoutingTable.DestinationAdvertisement entry : entries) {
		buffer.put(entry.destination.getBytes());
		buffer.putInt(entry.hops);
	    }

	    return new Packet(Packet.UPDATE_PACKET, localDevice.getBluetoothAddress(), 
			      "UPDATEPACKET", buffer.array());
	} finally {
	    mutex.readLock().unlock();
	}
    }

    public Packet requestUpdatePacket() {
        mutex.readLock().lock();
        try {
            Set<String> n = connections.keySet();
            ByteBuffer buf = ByteBuffer.allocate(4 + (n.size() * 12));
	    buf.putInt(n.size());
	    //System.out.println("number of connections: " + n.size());
            for (String i : n) {
                buf.put(i.getBytes());
		//System.out.println("adding: " + i);
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
		notifyListener("removeConnection", "removing connection: " + address);
                ConnectionHandler handler = connections.get(address);
                connections.remove(address);
                //routingTable.remove(localDevice.getBluetoothAddress(), address);
		routes.removeEntries(address);
                handler.setStop(true);
            }
        } finally {
            mutex.writeLock().unlock();
        }
    }

    public void handlePacket(byte[] p, String from) {
        Packet s = Packet.createPacket(p);
	notifyListener("handlePacket", "packet received from: " + from);
        s.decreaseHopCount();
        if (s.getHopCount() > 0) {
	    notifyListener("handlePacket", "hop count is still valid... processing");
            Log("SynCore", s.getSource() + " " + s.getDestination());
            if (s.getType() == Packet.DATA_PACKET) {
		notifyListener("handlePacket", "packet is [DATA]");
                handleDataPacket(s);
            } else if(s.getType() == Packet.UPDATE_PACKET) {
		notifyListener("handlePacket", "packet is [UPDATE]");
                handleUpdatePacket(s, from, true /* or false it does not matter */);
            }
        }
    }

    public void handleUpdatePacket(Packet p, String from, boolean t) {
	mutex.writeLock().lock();
	try {
	    ByteBuffer buffer = ByteBuffer.wrap(p.getPayload());
	    int limit = buffer.getInt();
	    int i = 0;
	    byte[] tmp = new byte[12];
	    while(i < limit) {
		// destination
		buffer.get(tmp);
		String destination = new String(tmp);
		int hops = buffer.getInt();
		routes.updateEntry(new RoutingTableEntry(destination, hops, from));
		i++;
	    }
	} finally {
	    mutex.writeLock().unlock();
	}
    }

    public void handleUpdatePacket(Packet p, String from) {
	mutex.writeLock().lock();
	try {
	    // dont update packets that came from this device
	    if (p.getSource().equals(localDevice.getBluetoothAddress()))
		return;
	
	    // get the md5 of the payload of the packet and store it
	    // if the previous md5 is the same with the new one
	    // then this packet is redundant
	    String hash = md5(p.getPayload());
	    if(history.containsKey(p.getSource())) {
		String old = history.get(p.getSource());
		if(old.equals(hash)) {
		    notifyListener("handleUpdatePacket", "packet is redundant... found match: " + hash);
		    for(String s : connections.keySet()) {
			if(!sent.containsKey(s) || !sent.get(s).equals(hash)) {
			    if (!s.equals(from)) {
				connections.get(s).offer(p);
				sent.put(s, hash);
			    }
			}
			return;
		    }
		}
	    }

	    notifyListener("handleUpdatePacket", "updating routing table...");
	    ByteBuffer buffer = ByteBuffer.wrap(p.getPayload());
	    //System.out.println(p.getPayload().length);
	    //buffer.put(p.getPayload());
	    routingTable.doSomethingImportant();
	    routingTable.remove(p.getSource());
	    routingTable.add(p.getSource());
	    byte[] tmp = new byte[12];
	    int limit = buffer.getInt();
	    //System.out.println("from: " + p.getSource() + " limit: " + limit);
	    int i = 0;
	    while (i < limit) {
		buffer.get(tmp, 0, 12);
		String tps = new String(tmp);
		if(!tps.equals(localDevice.getBluetoothAddress())) {
		    System.out.println("new device in the network: " + tps);
		    routingTable.add(p.getSource(), tps);
		}
		i++;
	    }

	    for (String address : connections.keySet()) {
		if (!address.equals(from)) {
		    connections.get(address).offer(p);
		}
	    }

	    notifyListener("handlerUpdatePacket", "keeping track of update packet");
	    //history.put(p.getSource(), hash);
	    networkChanged = true;
	} finally {
	    mutex.writeLock().unlock();
	}
    }

    public void handleDataPacket(Packet p) {
        // this packet is for this device
        if (p.getDestination().equals(localDevice.getBluetoothAddress())) {
	    notifyListener("handleDataPacket", "packet received for this device...");
	    //System.out.println(p.getSource() + " " + p.getPayload().length);
            offer(p);
        } else {
	    notifyListener("handleDataPacket", "routing packet");
	    System.out.println("routing packet from: " + p.getSource() + " destination: " + p.getDestination());
            //String nextHop = routingTable.nextHop(localDevice.getBluetoothAddress(), p.getDestination());
	    String nextHop = routes.nextHop(p.getDestination());
            if (nextHop != null) {
                Log("SynCore", "Packet from: " + p.getSource() + " routed to: " + nextHop);
		notifyListener("handleDataPacket", "Packet from: " + p.getSource() + " routed to: " + nextHop);
		System.out.println("Packet from: " + p.getSource() + " routed to: " + nextHop);
                for (String n : connections.keySet()) {
                    if (n.equals(nextHop)) {
                        connections.get(n).offer(p);
			break;
                    }
                }
            }
        }
    }

    public Set<String> getConnections() {
        mutex.readLock().lock();
        try {
	    routingTable.doSomethingImportant();
	    //System.out.println(routingTable.devices(localDevice.getBluetoothAddress()));
	    Set<String> res = routes.getConnections();
	    //System.out.println("connections: " + res);
            return res;
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
	    routingTable.doSomethingImportant();
            //String nextHop = routingTable.nextHop(localDevice.getBluetoothAddress(), destination);
	    String nextHop = routes.nextHop(destination);
            if (nextHop != null) {
                Packet p = new Packet(Packet.DATA_PACKET, localDevice.getBluetoothAddress(), destination, data);
                ConnectionHandler handler = connections.get(nextHop);
		//System.out.println("DAMN: " + data.length);
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

    public void setClientThreadListener(StateListener listener) {
	clientThread.setListener(listener);
    }

    public void setServerThreadListener(StateListener listener) {
	serverThread.setListener(listener);
    }

    public void setConnectionHandlerListener(String name, StateListener listener) {
	for(String n : connections.keySet())
	    if(name.equals(n)) {
		connections.get(n).setListener(listener);
		break;
	    }
    }

    public void setListener(StateListener listener) {
	this.listener = listener;
    }

    private void notifyListener(String state, String message) {
	if(listener != null)
	    listener.onStateChange(state, message);
    }

    public synchronized boolean hasChanged() {
	if(networkChanged) {
	    networkChanged = false;
	    return true;
	}
	return false;
    }
}
