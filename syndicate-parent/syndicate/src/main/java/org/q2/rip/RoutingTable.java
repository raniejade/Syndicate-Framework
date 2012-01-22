package org.q2.rip;

import java.util.*;

public class RoutingTable {
    public class DestinationAdvertisement {
	public final String destination;
	public int hops;

	private DestinationAdvertisement(String destination, int hops) {
	    this.destination = destination;
	    this.hops = hops;
	}
    }

    private final Vector<RoutingTableEntry> entries;

    public RoutingTable() {
	entries = new Vector<RoutingTableEntry>();
    }

    public void updateEntry(RoutingTableEntry entry) {
	// if the next hop is this device return
	//if(entry.next.equals(localhost))
	//  return;

	entry.hops++;
	RoutingTableEntry c = getEntry(entry.destination);
	if(c == null) {
	    entries.add(entry);
	} else {
	    // its a direct neighbor
	    // dont update
	    if(c.next.equals(c.destination)) {
		return;
	    }
	    else if(c.next.equals(entry.next)) {
		entries.remove(c);
		entries.add(entry);
	    } else if(entry.hops < c.hops){
		entries.remove(c);
		entries.add(entry);
	    }
	}
    }

    public void removeEntries(String next) {
	for(Iterator<RoutingTableEntry> it = entries.iterator(); it.hasNext();) {
	    RoutingTableEntry s = it.next();
	    if(s.next.equals(next))
		it.remove();
	}
    }

    public boolean contains(String destination) {
	for(RoutingTableEntry entry : entries) {
	    if(entry.destination.equals(destination))
		return true;
	}
	return false;	
    }

    private RoutingTableEntry getEntry(String destination) {
	for(RoutingTableEntry entry : entries) {
	    if(entry.destination.equals(destination))
		return entry;
	}
	return null;
    }

    public String nextHop(String destination) {
	RoutingTableEntry entry = getEntry(destination);
	return entry == null ? null : entry.next;
    }

    public Vector<DestinationAdvertisement> getAdvertisement(String destination) {
	Vector<DestinationAdvertisement> ret = new Vector<DestinationAdvertisement>();
	for(RoutingTableEntry entry : entries) {
	    if(!destination.equals(entry.next))
		ret.add(new DestinationAdvertisement(entry.destination, entry.hops));
	}
	return ret;
    }

    public Set<String> getConnections() {
	Set<String> res = new HashSet<String>();
	for(RoutingTableEntry entry : entries) {
	    res.add(entry.destination);
	}
	return res;
    }
}