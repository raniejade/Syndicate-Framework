package org.q2.syndicate;

import org.q2.bluetooth.BtConnection;
import org.q2.util.StateListener;

import javax.bluetooth.RemoteDevice;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.q2.util.DebugLog.Log;

final class ConnectionHandler extends Thread {
    private final BtConnection connection;
    private final RemoteDevice device;
    private volatile boolean stop;
    private final String tag;
    private final SyndicateCore synCore;

    private final ConcurrentLinkedQueue<Packet> queue;

    private StateListener listener;

    public ConnectionHandler(BtConnection connection) throws IOException {
        device = connection.getRemoteDevice();
        this.connection = connection;
        stop = false;
        tag = "Thread: [" + device.getBluetoothAddress() + "]";
        setName(tag);
        synCore = SyndicateCore.getInstance();
        queue = new ConcurrentLinkedQueue<Packet>();
	listener = null;
    }

    public synchronized void offer(Packet p) {
        queue.offer(p);
    }

    public void run() {
        Log(tag, "Started");
        long sendUpdate = 0;
        while (!stop) {
            boolean connected = true;
            synCore.getLock().lock();
            try {
                device.getFriendlyName(true);
		notifyListener("verify", "contacting remote device");
            } catch (IOException e) {
                Log(tag, e.getMessage());
                synCore.removeConnection(device.getBluetoothAddress());
                connected = false;
            } catch (RuntimeException e) {
                Log(tag, e.getMessage());
                synCore.removeConnection(device.getBluetoothAddress());
                connected = false;
            } finally {
                synCore.getLock().unlock();
            }

            if (connected) {

                if (System.currentTimeMillis() - sendUpdate > 6000) {
		    notifyListener("send update","sending update packet");
                    synCore.getLock().lock();
                    try {
                        sendUpdatePacket();
                        sendUpdate = System.currentTimeMillis();
                    } catch (IOException e) {
                        Log(tag, e.getMessage());
                        synCore.removeConnection(device.getBluetoothAddress());
                    } finally {
                        synCore.getLock().unlock();
                    }


                }
                synCore.getLock().lock();
                try {
                    byte[] rec = connection.receive();
		    notifyListener("receiving data", "attempting to receive data");
                    if (rec != null) {
			notifyListener("data received", "calling synCore.handlePacket");
                        synCore.handlePacket(rec, device.getBluetoothAddress());
                    }
                } catch (IOException e) {
                    Log(tag, e.getMessage());
                    synCore.removeConnection(device.getBluetoothAddress());
                } finally {
                    synCore.getLock().unlock();
                }

                synCore.getLock().lock();
                try {
                    if (!queue.isEmpty()) {
                        Packet p = queue.poll();
			notifyListener("sending data", "destination: " + p.getDestination() + " from: " + p.getSource());
                        connection.send(p.toBytes());
                    }
                } catch (IOException e) {
                    synCore.removeConnection(device.getBluetoothAddress());
                    Log(tag, e.getMessage());
                } finally {
                    synCore.getLock().unlock();
                }
            }
        }

        try {
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        Log(tag, "Finished");
    }

    public String getBtAddress() {
        return device.getBluetoothAddress();
    }

    public synchronized void setStop(boolean t) {
        stop = t;
    }

    public void sendUpdatePacket() throws IOException {
        Packet p = synCore.requestUpdatePacket();
        connection.send(p.toBytes());
        Log(tag, "Update packet sent");
    }

    public int hashCode() {
        return device.getBluetoothAddress().hashCode();
    }

    public boolean equals(Object o) {
        return hashCode() == o.hashCode();
    }

    public void setListener(StateListener listener) {
	this.listener = listener;
    }

    public void notifyListener(String state, String message) {
	if(listener != null)
	    listener.onStateChange(state, message);
    }
}
