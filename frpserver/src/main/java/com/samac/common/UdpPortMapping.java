package com.samac.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class UdpPortMapping implements PortUseBase{
    private short index = 0;
    private String mark;
    private int serverPort;
    private String localAddress;
    private int localPort;
    private boolean flip = false;

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public void setFlip(boolean flip) {
        this.flip = flip;
    }

    public boolean isFlip() {
        return flip;
    }

    public void setIndex(short index) {
        this.index = index;
    }

    @Override
    public short getIndex() {
        return index;
    }

    @Override
    public int getPort() {
        return serverPort;
    }

    @Override
    public byte getType() {
        return 0;
    }

    public ByteBuf getBytes() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(index);
        byte[] markData = mark.getBytes();
        buf.writeInt(markData.length);
        buf.writeBytes(markData);
        buf.writeInt(serverPort);
        byte[] localAddrData = localAddress.getBytes();
        buf.writeInt(localAddrData.length);
        buf.writeBytes(localAddrData);
        buf.writeInt(localPort);
        buf.writeBoolean(flip);

        return buf;
    }

    public void setBytes(ByteBuf buf) {
        index = buf.readShort();
        int markLen = buf.readInt();
        byte[] markData = new byte[markLen];
        buf.readBytes(markData);
        mark = new String(markData);
        serverPort = buf.readInt();
        int localAddrLen = buf.readInt();
        byte[] localAddrData = new byte[localAddrLen];
        buf.readBytes(localAddrData);
        localAddress = new String(localAddrData);
        localPort = buf.readInt();
        flip = buf.readBoolean();
    }
}
