package org.q2.qtransfer;

import org.q2.syndicate.*;
import java.nio.ByteBuffer;

import java.util.*;


class RequestHandler extends Thread {
    public class Identity {
	public String name;
	public String address;
	private Identity(String name, String address) {
	    this.name = name;
	    this.address = address;
	}
    }

    private static final byte IDENTIFY_REQUEST = 0x0;
    private static final byte IDENTIFY_REPLY = 0x1;
    private static final byte REQUEST_TRANSFER = 0x2;
    private static final byte REQUEST_ACCEPTED = 0x3;
    private static final byte REQUEST_REJECTED = 0x4;
    private final QTransferGUI handle;

    private volatile boolean acceptIdentityReply;
    private final Vector<Identity> identities;

    public RequestHandler(QTransferGUI handle)  {
	super("Request Handler");
	this.handle = handle;
	setDaemon(true);
	acceptIdentityReply = false;
	identities = new Vector<Identity>();
    }

    public void startAcceptIdentityReply() {
	try {
	    SCC scc = SCC.getInstance();
	    System.out.println("SENDING CAPSSSS");
	    Set<String> connections = scc.getConnections();
	    System.out.println("connections: " + connections);
	    for(String s : connections) {
		Connection con = scc.openConnection(s);
		if(con != null) {
		    ByteBuffer buffer = ByteBuffer.allocate(1);
		    buffer.put(IDENTIFY_REQUEST);
		    try {
			con.send(buffer.array());
			System.out.println("sent to: " + s);
		    } catch (DestinationUnreachableException e) {
			e.printStackTrace();
		    }
		}
	    }
	    acceptIdentityReply = true;
	    identities.clear();
	} catch (RuntimeException e ) {
	    System.out.println(e.getMessage());
	}

	} 

    public Vector<Identity> stopAcceptIdentityReply() {
	acceptIdentityReply = false;
	return identities;
    }


    public void run() {
	while(true) {
	    SCC scc = SCC.getInstance();
	    SCC.Data data = scc.receive();

	    if(data != null) {
		if(data.data.length > 0) {
		    if(data.data[0] == IDENTIFY_REQUEST) {
			Connection con = scc.openConnection(data.source);
			if(con != null) {
			    String name = handle.getName();
			    ByteBuffer buffer = ByteBuffer.allocate(1 + name.length());
			    buffer.put(IDENTIFY_REPLY);
			    buffer.put(name.getBytes());
			    //System.out.println(name + " " + name.length());
			    System.out.println("identify request received from: " + data.source);
			    try {
				con.send(buffer.array());
			    } catch (DestinationUnreachableException e) {
				e.printStackTrace();
			    }
			}
		    } else if(data.data[0] == REQUEST_TRANSFER) {
		    } else if(data.data[0] == REQUEST_ACCEPTED) {
		    } else if(data.data[0] == REQUEST_REJECTED) {
		    } else if(data.data[0] == IDENTIFY_REPLY) {
			if(acceptIdentityReply) {
			    //System.out.println(data.data.length);
			    String name = new String(data.data, 1, data.data.length - 1);
			    Identity id = new Identity(name, data.source);
			    identities.add(id);
			}
		    }
		}
	    }
	}
    }
}
