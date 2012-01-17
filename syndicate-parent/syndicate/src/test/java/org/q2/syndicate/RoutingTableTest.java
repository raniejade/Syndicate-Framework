package org.q2.syndicate;

import junit.framework.*;

public class RoutingTableTest extends TestCase {
    private RoutingTable table;
    public RoutingTableTest() {
	super("org.q2.syndicate.RoutingTable");
	table = new RoutingTable();
    }

    protected void setUp() {
	table.add("localhost");
	table.add("localhost", "remote1");
	table.add("localhost", "remote2");
	table.add("remote1");
	table.add("remote1", "remote3");
	table.add("remote2");
	table.add("remote2", "remote4");
	table.add("remote4");
	table.add("remote4", "remote5");
	table.add("remote5");
	table.add("remote5", "remote6");
    }

    public void testNextHop1() {
	assertEquals(table.nextHop("localhost", "remote1"), "remote1");
    }

    public void testNextHop2() {
	assertEquals(table.nextHop("localhost", "remote3"), "remote1");
    }

    public void testNextHop3() {
	assertEquals(table.nextHop("localhost", "remote6"), "remote2");
    }

    protected void tearDown() {
	table.remove("localhost");
    }
}