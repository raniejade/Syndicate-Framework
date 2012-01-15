package org.q2.bluetooth;

import java.io.IOException;

/**
 * This class is responsible for creating SDP records and listen for incoming connections
 */
public interface BtConnectionNotifier {

    /**
     * Listen and wait until a client connects
     *
     * @return {@link BtConnection} between this device and the connecting device
     * @throws IOException when this notifier is closed
     */
    public BtConnection acceptAndOpen() throws IOException;

    /**
     * Close this notifier. Note that closing a notifier will make any call to <code>acceptAndOpen</code> throw an {@link IOException}
     *
     * @throws IOException an error occurred
     */
    public void close() throws IOException;
}
