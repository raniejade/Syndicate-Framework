package org.q2.syndicate;

import org.q2.bluetooth.BtConnection;
import org.q2.bluetooth.BtConnectionNotifier;

import java.io.IOException;

import static org.q2.util.DebugLog.Log;

final class ServerThread extends Thread {
    public static final String TAG = "Syndicate Server Thread";
    private final SyndicateCore synCore;
    private BtConnectionNotifier service;
    private volatile boolean stop;

    public ServerThread() {
        super(TAG);
        setDaemon(true);
        synCore = SyndicateCore.getInstance();
        stop = false;
    }

    public void run() {
        try {
            startService();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        Log(TAG, "server thread started");

        while (!stop) {
            try {
                BtConnection connection = service.acceptAndOpen();
                Log(TAG, "Waiting....");
                synCore.getLock().lock();
                try {
                    synCore.acceptConnection(connection, true);
                    synCore.setMaster(true);
                } finally {
                    synCore.getLock().unlock();
                }
            } catch (IOException e) {
                Log(TAG, "service closed");
            }
        }

        stopService();
    }

    public synchronized void setStop(boolean t) {
        stop = t;
    }

    private void startService() throws IOException {
        service = synCore.openNotifier(synCore.SYNDICATE_UUID.toString());
    }

    public void stopService() {
        try {
            service.close();
        } catch (IOException e) {
            Log(TAG, e.getMessage());
        }
    }
}
