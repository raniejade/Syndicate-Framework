package org.q2.bluetooth;

import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import java.io.IOException;

final class BtRFCOMMConnectionNotifier implements BtConnectionNotifier {
    private final StreamConnectionNotifier notifer;

    public BtRFCOMMConnectionNotifier(StreamConnectionNotifier notifier) {
        this.notifer = notifier;
    }

    @Override
    public BtConnection acceptAndOpen() throws IOException {
        return new BtRFCOMMConnection((StreamConnection) notifer.acceptAndOpen());
    }

    @Override
    public void close() throws IOException {
        notifer.close();
    }
}
