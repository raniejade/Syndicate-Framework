package org.q2.qtransfer;

class Segment {
    public final byte[] data;
    public int size;
    
    public Segment(byte[] data) {
	size = data.length;
	this.data = new byte[size];
	System.arraycopy(data, 0, this.data, 0, size);
    }
}