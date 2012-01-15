package org.q2.bluetooth;

import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.L2CAPConnectionNotifier;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import java.io.IOException;

/**
 * Responsible for opening connections and connection notifiers
 */
public final class BtConnector {
    protected BtConnector() {

    }

    /**
     * Create a new RFCOMM connection
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static BtConnection openRFCOMMConnection(String url) throws IOException {
        return new BtRFCOMMConnection((StreamConnection) Connector.open(url));
    }

    /**
     * Create a new L2CAP connection
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static BtConnection openL2CAPConnection(String url) throws IOException {
        return new BtL2CAPConnection((L2CAPConnection) Connector.open(url));
    }

    /**
     * Open a RFCOMM notifier
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static BtConnectionNotifier openRFCOMMConnectionNotifier(String url) throws IOException {
        return new BtRFCOMMConnectionNotifier((StreamConnectionNotifier) Connector.open(url));
    }

    /**
     * Open a L2CAP notifier
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static BtConnectionNotifier openL2CAPConnectionNotifier(String url) throws IOException {
        return new BtL2CAPConnectionNotifier((L2CAPConnectionNotifier) Connector.open(url));
    }
}
