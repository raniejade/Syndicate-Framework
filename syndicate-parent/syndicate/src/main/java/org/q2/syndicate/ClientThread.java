package org.q2.syndicate;

import org.q2.bluetooth.BtConnection;

import org.q2.util.StateListener;

import javax.bluetooth.*;
import java.io.IOException;

import static org.q2.util.DebugLog.Log;

final class ClientThread extends Thread implements DiscoveryListener {
    private static final String TAG = "Syndicate Client Thread";
    private final SyndicateCore synCore;
    private volatile boolean stop;
    private final DiscoveryAgent agent;

    private volatile RemoteDevice found;

    private final Object monitor;

    private final UUID[] uuids;

    private volatile Integer transactionID;

    private volatile ServiceRecord serviceRecord;

    private StateListener listener;

    public ClientThread() {
        super(TAG);
        synCore = SyndicateCore.getInstance();
        stop = false;
        agent = synCore.getLocalDevice().getDiscoveryAgent();
        monitor = new Object();
        uuids = new UUID[1];
        uuids[0] = synCore.SYNDICATE_UUID;
        setDaemon(true);
	listener = null;
    }

    @Override
    public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass deviceClass) {
        if (!synCore.hasConnection(remoteDevice.getBluetoothAddress())) {
            found = remoteDevice;
            agent.cancelInquiry(this);
        }
    }

    @Override
    public void servicesDiscovered(int i, ServiceRecord[] serviceRecords) {
        if (serviceRecords.length > 0 && transactionID == i) {
            serviceRecord = serviceRecords[0];
            agent.cancelServiceSearch(transactionID);
        }
    }

    @Override
    public void serviceSearchCompleted(int i, int i1) {
        synchronized (monitor) {
            monitor.notify();
        }
    }

    @Override
    public void inquiryCompleted(int i) {
        synchronized (monitor) {
            monitor.notify();
        }
    }

    public void run() {
        Log(TAG, "client thread started");
        while (!stop) {
            try {
                found = null;
                agent.startInquiry(DiscoveryAgent.GIAC, this);
		notifyListener("inquiry", "inquiry started");
                // wait until
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {

                    }
                }

		notifyListener("inquiry", "inquiry done");

                // found a device
                if (found != null) {
		    notifyListener("inquiry", "found device: " + found.getBluetoothAddress());
                    serviceRecord = null;
		    notifyListener("service search", "searching Syndicate service on: " + found.getBluetoothAddress());
                    transactionID = agent.searchServices(null, uuids, found, this);

                    synchronized (monitor) {
                        try {
                            monitor.wait();
                        } catch (InterruptedException e) {

                        }
                    }

		    notifyListener("service search", "searching Syndicate service done");

                    if (serviceRecord != null) {
                        String connectionURL = serviceRecord.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
			notifyListener("service search", "Syndicate service found: " + connectionURL);
                        if (synCore.getLock().tryLock()) {
                            try {
				notifyListener("establish connection", "attempting to establish connection..");
                                BtConnection connection = synCore.openConnection(connectionURL);
                                Log(TAG, "Waiting....");
				notifyListener("establish connection", "calling synCore.acceptConnection");
                                synCore.acceptConnection(connection, false);
                                Log(TAG, "new connection - " + connection.getRemoteDevice().getBluetoothAddress());
                            } catch (IOException e) {
				notifyListener("establish connection", "connection failed...");
                                Log(TAG, e.getMessage());
                            } finally {
                                synCore.getLock().unlock();
                            }
                        }
                    }
                }
            } catch (BluetoothStateException e) {
                Log(TAG, "failed to start device inquiry - " + e.getMessage());
		notifyListener("inquiry", "failed to start inquiry.. " + e.getMessage());
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
        }
    }

    public synchronized void setStop(boolean t) {
        stop = t;
    }

    public void setListener(StateListener listener) {
	this.listener = listener;
    }

    private void notifyListener(String state, String message) {
	if(listener != null)
	    listener.onStateChange(state, message);
    }
}
