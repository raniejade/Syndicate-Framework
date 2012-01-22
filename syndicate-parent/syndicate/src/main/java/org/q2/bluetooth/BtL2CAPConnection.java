package org.q2.bluetooth;

import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.RemoteDevice;
import java.io.IOException;

/**
 * A L2CAP connection
 */
final class BtL2CAPConnection implements BtConnection {
    private final L2CAPConnection connection;

    public BtL2CAPConnection(L2CAPConnection connection) {
        this.connection = connection;
    }

    @Override
    public void send(byte[] data) throws IOException, NullPointerException {
        if (data != null) {
            connection.send(data);
        } else {
            throw new NullPointerException();
        }
    }

    @Override
    public byte[] receive() throws IOException {
        if (connection.ready()) {
            byte[] rec = new byte[connection.getReceiveMTU()];
            int size = connection.receive(rec);
	    byte[] ret = new byte[size];
	    System.arraycopy(rec, 0, ret, 0, size);
            return rec;
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }

    @Override
    public RemoteDevice getRemoteDevice() throws IOException {
        return RemoteDevice.getRemoteDevice(connection);
    }
}
