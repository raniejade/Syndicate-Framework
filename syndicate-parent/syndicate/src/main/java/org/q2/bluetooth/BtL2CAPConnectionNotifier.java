package org.q2.bluetooth;

import javax.bluetooth.L2CAPConnectionNotifier;
import java.io.IOException;

/**
 * Notifier for L2CAP connections
 */
final class BtL2CAPConnectionNotifier implements BtConnectionNotifier {
    private final L2CAPConnectionNotifier notifier;

    public BtL2CAPConnectionNotifier(L2CAPConnectionNotifier notifier) {
        this.notifier = notifier;
    }

    @Override
    public BtConnection acceptAndOpen() throws IOException {
        return new BtL2CAPConnection(notifier.acceptAndOpen());
    }

    @Override
    public void close() throws IOException {
        notifier.close();
    }
}
