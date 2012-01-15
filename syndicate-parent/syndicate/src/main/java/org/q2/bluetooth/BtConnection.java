package org.q2.bluetooth;

import javax.bluetooth.RemoteDevice;
import java.io.IOException;

/**
 * Represents a Bluetooth connection
 */
public interface BtConnection {
    /**
     * Send <code>data</code> to the {@link javax.bluetooth.RemoteDevice} at the end of this connection.
     *
     * @param data
     * @throws IOException          I/O error happen, or connection was closed.
     * @throws NullPointerException <code>data</code> is <code>null</code>.
     */
    public void send(byte[] data) throws IOException, NullPointerException;

    /**
     * Attempt to receive data
     *
     * @return data sent by {@link javax.bluetooth.RemoteDevice}, or <code>null</code> if there is nothing to receive
     * @throws IOException I/O error occurred, or connection was closed
     */
    public byte[] receive() throws IOException;


    /**
     * Close this connection
     *
     * @throws IOException
     */
    public void close() throws IOException;

    /**
     * Retrieve the device at the end of this connection
     *
     * @return
     * @throws IOException
     */
    public RemoteDevice getRemoteDevice() throws IOException;
}
