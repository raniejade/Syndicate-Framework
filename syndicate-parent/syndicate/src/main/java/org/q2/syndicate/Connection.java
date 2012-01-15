package org.q2.syndicate;


/**
 * A remote connection to a specific device in the network that can be used
 * to send data.
 */
public abstract class Connection {
    private final String remoteAddress;

    public Connection(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    /**
     * Send <code>data</code> to the remote device.
     *
     * @throws DestinationUnreachableException
     *          when the link to this connections gets lost. Link lost can be caused by hop-nodes
     *          between the local device and remote device get lost. Due to the adhoc nature of the network, when this exception is thrown
     *          it does not mean that this connection is not valid anymore. Nodes in this network might rendezvous with this device producing
     *          another communication link.
     */
    public abstract void send(byte[] data) throws DestinationUnreachableException;

    /**
     * Retrieve the address of the remote device of this connection
     */
    public String getAddress() {
        return remoteAddress;
    }
}