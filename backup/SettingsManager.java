package org.q2.qtransfer;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.util.Scanner;

final class SettingsManager {
    private SettingsManager(String name, int segmentSize, int timeout) {
	this.name = name;
	this.segmentSize = segmentSize;
	this.timeout = timeout;
    }

    // name of this device
    // generally every qtransfer instance is associated with a name
    // that is shared to the other instances in the network
    private String name;

    // qtransfer splits a file into several segments.
    // this property specifies the maximum size of a segment
    private int segmentSize;

    // during a transfer a device might disappear (device(s) connected to it might leave the network)
    // this property is default timeout that qtransfer
    // will wait for the device to appear again in the network
    private int timeout;

    public static SettingsManager createDefault() {
	return new SettingsManager("QTransfer", 256, 10000);
    }

    public static SettingsManager load(String filename) {
	try {
	    Scanner sc = new Scanner(new File(filename));
	    String name = sc.nextLine();
	    int segmentSize = Integer.parseInt(sc.nextLine());
	    int timeout = Integer.parseInt(sc.nextLine());
	    return new SettingsManager(name, segmentSize, timeout);
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	    return null;
	}
    }

    public static void save(SettingsManager sm, String filename) throws FileNotFoundException  {
	PrintWriter out = new PrintWriter(new File(filename));
	out.println(sm.name);
	out.println(String.valueOf(sm.segmentSize));
	out.println(String.valueOf(sm.timeout));
	out.close();
    }

    public static boolean checkIfExists(String filename) {
	return new File(filename).exists();
    }

    public String getName() {
	return name;
    }
    
    public int getSegmentSize() {
	return segmentSize;
    }

    public int getTimeout() {
	return timeout;
    }

    public void setName(String n) {
	name = n;
    }

    public void setSegmentSize(int s) {
	segmentSize = s;
    }

    public void setTimeout(int t) {
	timeout = t;
    }
}