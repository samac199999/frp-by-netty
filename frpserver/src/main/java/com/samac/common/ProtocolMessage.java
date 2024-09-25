package com.samac.common;

import io.netty.buffer.ByteBuf;

public class ProtocolMessage {
    public static final int CHECK_NUMBER = 0xF3141592;
    public static final byte P_CHECK_AUTH = 0x01;
    public static final byte P_PORT_MAPPING = 0x02;
    public static final byte P_HEART_BEAT = 0x03;
    public static final byte P_DATA_TRANSFER = 0x04;
    public static final byte P_CONNECT_FAIL = 0x05;
    public static final byte P_CONNECT_SUCCESS = 0x06;
    public static final byte P_DISCONNECT = 0x07;
    public static final byte P_CONNECT = 0x08;
    public static final byte P_SERVER_ERROR_MSG = 0x09;
    public static final byte P_TEST_MSG = 0x0A;
    public static final byte P_SOCKS5_CONF = 0x0B;
    public static final byte P_SOCKS5_CONNECT = 0x0C;
    public static final byte P_TRANS_DATA_ACK = 0x0D;
    public static final byte P_SOCKS5_UDP_CONF = 0x0E;
    public static final byte P_UDP_DATA_TRANSFER = 0x10;
    public static final byte P_UDP_PORT_MAPPING = 0x11;
    public static final byte P_ENCRYPT_KEY = 0x12;
    public static final byte P_HEART_BEAT_ACK = 0x13;

    private int checkNumber = CHECK_NUMBER;
    private byte protocol = P_HEART_BEAT;
    private int dataLength = 0;
    private int reserved = 0;
    private int reserved1 = 0;
    private byte needAck = 0;

    private ByteBuf data = null;

    public int getCheckNumber() {
        return checkNumber;
    }

    public void setCheckNumber(int checkNumber) {
        this.checkNumber = checkNumber;
    }

    public byte getProtocol() {
        return protocol;
    }

    public void setProtocol(byte protocol) {
        this.protocol = protocol;
    }

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    public int getReserved() {
        return reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    public void setReserved1(int reserved1) {
        this.reserved1 = reserved1;
    }

    public int getReserved1() {
        return reserved1;
    }

    public void setNeedAck(byte needAck) {
        this.needAck = needAck;
    }

    public byte getNeedAck() {
        return needAck;
    }

    public ByteBuf getData() {
        return data;
    }

    public void setData(ByteBuf data) {
        data.retain();
        this.dataLength = data.readableBytes();
        this.data = data;
    }

    public void releaseBuf() {
        if (this.data != null) {
            this.data.release();
            this.data = null;
        }
    }
}
