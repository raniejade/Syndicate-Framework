package org.q2.syndicate;

import org.q2.bluetooth.BtConnection;
import org.q2.bluetooth.BtConnectionNotifier;

import java.io.IOException;

import org.q2.util.StateListener;

import static org.q2.util.DebugLog.Log;

final class ServerThread extends Thread {
    public static final String TAG = "Syndicate Server Thread";
    private final SyndicateCore synCore;
    private BtConnectionNotifier service;
    private volatile boolean stop;

    private StateListener listener;

    public ServerThread() {
        super(TAG);
        setDaemon(true);
        synCore = SyndicateCore.getInstance();
        stop = false;
	    listener = null;
        setPriority(Thread.MAX_PRIORITY);
    }

    public void run() {
        Log(TAG, "server thread started");
        try {
            startService();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }


        while (!stop) {
            //synCore.getLock().lock();
            try {
                /*Thread t = new Thread(new Runnable() {
                    public void run() {
                        long start = System.currentTimeMillis();
                        while(System.currentTimeMillis() - start <= 4000);

                        stopService();
                    }
                });
                t.setDaemon(true);
                t.start();*/
                
		        notifyListener("Listening", "waiting for new connections..");
                BtConnection connection = service.acceptAndOpen();
                Log(TAG, "Waiting....");
		        notifyListener("new connection", "received new connection");
                synCore.getLock().lock();
                try {
                    synCore.acceptConnection(connection, true);
		        // synCore.setMaster(true);
                } finally {
                    synCore.getLock().unlock();
                }
            } catch (IOException e) {
                Log(TAG, "service closed");
            } /*finally {
                synCore.getLock().unlock();
            }*/

            /*try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
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

    public void setListener(StateListener listener) {
	this.listener = listener;
    }

    private void notifyListener(String state, String message) {
	if(listener != null)
	    listener.onStateChange(state, message);
    }
}
