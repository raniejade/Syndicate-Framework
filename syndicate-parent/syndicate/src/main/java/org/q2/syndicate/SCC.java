package org.q2.syndicate;

import java.util.Set;

/**
 * Syndicate Control Center, this class responsible for establishing connections
 * and receiving data.
 */
public final class SCC {
    private static final SCC instance = new SCC();

    public static SCC getInstance() {
        return instance;
    }

    private final SyndicateCore synCore;

    private SCC() {
        synCore = SyndicateCore.getInstance();
        synCore.initialize();
    }

    public class Data {
        public final String source;
        public final byte[] data;

        private Data(String source, byte[] data) {
            this.source = source;
            this.data = new byte[data.length];
	    System.arraycopy(data, 0, this.data, 0, data.length);
        }
    }

    public Connection openConnection(String address) {
        if (synCore.hasConnection(address)) {
            return new ConnectionImpl(address);
        }
        return null;
    }

    /**
     * Attempt to receive data
     *
     * @return {@link SCC.Data} or <code>null</code> if nothing is to be received.
     */
    public SCC.Data receive() {
        Packet p = synCore.poll();
        if (p != null) {
            return new SCC.Data(p.getSource(), p.getPayload());
        }
        return null;
    }

    /**
     * Retrieve the addresses of all devices in the network.
     *
     * @return List of connection addresses.
     */
    public Set<String> getConnections() {
        return synCore.getConnections();
    }

    private class ConnectionImpl extends Connection {
        private ConnectionImpl(String address) {
            super(address);
        }

        @Override
        public void send(byte[] data) throws DestinationUnreachableException {
            if (!synCore.send(getAddress(), data)) {
                throw new DestinationUnreachableException(getAddress());
            }
        }
    }
}