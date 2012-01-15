package org.q2.syndicate;

import java.util.HashMap;
import java.util.Vector;
import java.util.HashSet;
import java.util.Set;

final class RoutingTable {
    private final HashMap<String, Vector<String>> table;

    public RoutingTable() {
        table = new HashMap<String, Vector<String>>();
    }

    public void add(String name) {
        if (!table.containsKey(name)) {
            table.put(name, new Vector<String>());
        }
    }

    public void add(String row, String name) {
        if (table.containsKey(row)) {
            table.get(row).add(name);
        }
    }

    public void remove(String name) {
        if (table.containsKey(name)) {
            for (String n : table.get(name)) {
                remove(n);
            }
            table.remove(name);
        }
    }

    public void remove(String row, String name) {
        if (table.containsKey(row)) {
            if (table.get(row).contains(name)) {
                remove(name);
                table.get(row).remove(name);
            }
        }
    }

    public boolean search(String row, String name) {
        if (table.containsKey(row)) {
            if (row.equals(name)) {
                //Log("RoutingTable", "MATCH");
                return true;
            }
            for (String n : table.get(row)) {
                if (search(n, name))
                    return true;
            }
        }
        //Log("RoutingTable", "NO MATCH");
        return false;
    }

    public String nextHop(String row, String name) {
        if (table.containsKey(row)) {
            if (row.equals(name)) {
                return row;
            }

            for (String n : table.get(row)) {
                if (nextHop(n, name) != null)
                    return n;
            }
        }
        return null;
    }

    public Set<String> devices(String localDevice) {
	Set<String> rec = new HashSet<String>();
	return devices(rec, localDevice);
    }

    private Set<String> devices(Set<String> rec, String current) {
	if(table.containsKey(current)) {
	    for(String n : table.keySet()) {
		Vector<String> t = table.get(n);
		if(!rec.contains(n)) {
		    rec.add(n);
		}
		devices(rec, n);
	    }
	}
	return rec;
    }
}
