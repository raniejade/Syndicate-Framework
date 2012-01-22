package org.q2.syndicate;

import java.nio.ByteBuffer;

final class Packet {
    public final static Byte DATA_PACKET = 0x1;
    public final static Byte UPDATE_PACKET = 0x2;

    private final Byte type;
    private final String source;
    private final String destination;
    private final byte[] payload;
    private byte hopCount;

    private int payloadSize;

    public static Packet createPacket(byte[] data) {

	//System.out.println("createPacket: " + data.length);
        ByteBuffer buffer = ByteBuffer.wrap(data);

        Byte type = buffer.get();

        byte[] tmp = new byte[12];

        buffer.get(tmp);
        String source = new String(tmp);

        buffer.get(tmp);
        String destination = new String(tmp);

        Byte hopCount = buffer.get();

	int payloadSize = buffer.getInt();

        byte[] payload = new byte[(int)payloadSize];
	//System.out.println("payload: " + payloadSize);
        buffer.get(payload);

        return new Packet(type, source, destination, payload, hopCount);
    }

    public Packet(Byte type, String source, String destination, byte[] payload) {
        this.type = type;
        this.source = source;
        this.destination = destination;
        hopCount = 8;
        this.payload = new byte[payload.length];
	this.payloadSize = payload.length;
        System.arraycopy(payload, 0, this.payload, 0, payload.length);
    }

    public Packet(Byte type, String source, String destination, byte[] payload, byte hopCount) {
        this.type = type;
        this.source = source;
        this.destination = destination;
        this.hopCount = hopCount;
        this.payload = new byte[payload.length];
	this.payloadSize = payload.length;
        System.arraycopy(payload, 0, this.payload, 0, payload.length);
    }

    public Byte getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public byte[] getPayload() {
        return payload;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(30 + payloadSize);
        buffer.put(type);
        buffer.put(source.getBytes());
        buffer.put(destination.getBytes());
        buffer.put(hopCount);
	buffer.putInt(payloadSize);
        buffer.put(payload);
        return buffer.array();
    }

    public byte getHopCount() {
        return hopCount;
    }

    public void decreaseHopCount() {
        hopCount--;
    }
}
