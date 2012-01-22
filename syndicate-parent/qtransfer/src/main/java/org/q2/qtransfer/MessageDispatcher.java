package org.q2.qtransfer;

import org.q2.syndicate.*;

import java.nio.*;
import java.util.*;

class MessageDispatcher extends Thread {
    public class Identity {
	public final String name;
	public final String address;

	private Identity(String name, String address) {
	    this.name = name;
	    this.address = address;
	}
    }

    public final byte IDENTITY_REQUEST = 0x0;
    public final byte IDENTITY_REPLY = 0x1;

    private final QTransferGUI handle;

    private volatile boolean requestIdentity;
    private final Vector<Identity> identities;

    public MessageDispatcher(QTransferGUI handle) {
	super("Message Dispatcher");
	setDaemon(true);
	this.handle = handle;
	requestIdentity = false;
	identities = new Vector<Identity>();
    }

    public void startRequestIdentity() {
	requestIdentity = true;
	identities.clear();
	SCC scc = SCC.getInstance();

	Set<String> connections = scc.getConnections();
	for(String c : connections) {
	    Connection con = scc.openConnection(c);
	    try {
		byte[] data = new byte[1];
		data[0] = IDENTITY_REQUEST;
		con.send(data);
	    } catch (DestinationUnreachableException e) {
		e.printStackTrace();
	    }
	}
    }

    public Vector<Identity> stopRequestIdentity() {
	requestIdentity = false;
	return identities;
    }

    public void run() {
	SCC scc = SCC.getInstance();
	while(true) {
	    SCC.Data rec = scc.receive();

	    if(rec == null)
		continue;

	    if(rec.data[0] == IDENTITY_REQUEST) {
		System.out.println("received identity request from: " + rec.source);
		Connection con = scc.openConnection(rec.source);
		if(con != null) {
		    String name = handle.getName();
		    ByteBuffer buffer = ByteBuffer.allocate(1 + name.length());
		    buffer.put(IDENTITY_REPLY);
		    buffer.put(name.getBytes());
		    try {
			// send
			con.send(buffer.array());
		    } catch (DestinationUnreachableException e) {
			e.printStackTrace();
		    }
		}
	    } else if(rec.data[0] == IDENTITY_REPLY) {
		if(requestIdentity) {
		    String name = new String(rec.data, 1, rec.data.length - 1);
		    identities.add(new Identity(name, rec.source));
		    System.out.println("received identity reply from: " + name + "(" + rec.source + ")");
		}
	    }
	}
    }
}