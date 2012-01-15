package org.q2.bluetooth;

import javax.bluetooth.RemoteDevice;
import javax.microedition.io.StreamConnection;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

final class BtRFCOMMConnection implements BtConnection {
    private final StreamConnection connection;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;

    public BtRFCOMMConnection(StreamConnection connection) throws IOException {
        this.connection = connection;
        inputStream = connection.openDataInputStream();
        outputStream = connection.openDataOutputStream();
    }

    @Override
    public void send(byte[] data) throws IOException, NullPointerException {
        if (data != null) {
            outputStream.write(data);
        } else {
            throw new NullPointerException();
        }
    }

    @Override
    public byte[] receive() throws IOException {
        int available = inputStream.available();
        if (available > 0) {
            byte[] rec = new byte[available];
            inputStream.read(rec);
            return rec;
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        connection.close();
        try {
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {

        }
    }

    @Override
    public RemoteDevice getRemoteDevice() throws IOException {
        return RemoteDevice.getRemoteDevice(connection);
    }
}
