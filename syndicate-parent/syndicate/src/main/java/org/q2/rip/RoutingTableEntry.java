package org.q2.rip;

public class RoutingTableEntry {
    public final String destination;
    public int hops;
    public final String next;

    public RoutingTableEntry(String destination, int hops, String next) {
	this.destination = destination;
	this.hops = hops;
	this.next = next;
    }

    public int hashCode() {
	return destination.hashCode();
    }

    public boolean equals(Object o) {
	return hashCode() == o.hashCode();
    }
}