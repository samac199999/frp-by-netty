package com.samac.common;

/*
 * Copyright (C) 2024 samac199999
 * All rights reserved.
 *
 * Contact: samac199999@gmail.com
 *
 * This code is licensed under the MIT License.
 * See the LICENSE file in the project root for more information.
 */

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class MessagePacketDecoder extends LengthFieldBasedFrameDecoder
{
    public static final int HEADER_SIZE = (4 + 1 + 4 + 4 + 4 + 1); // 13
    public static final int MAX_FRAME_LENGTH = 1024 * 1024 + HEADER_SIZE;
    public static final int LENGTH_FIELD_OFFSET = 5;
    public static final int LENGTH_FIELD_LENGTH = 4;
    public static final int LENGTH_ADJUSTMENT = 9;
    public static final int BYTES_TO_STRIP = 0;

    public MessagePacketDecoder() {
        super(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, BYTES_TO_STRIP);
    }

    @Override
    protected ProtocolMessage decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        in = (ByteBuf) super.decode(ctx, in);

        if (in == null) {
            return null;
        }

        if (in.readableBytes() < HEADER_SIZE) {
            in.release();
            return null;
        }

        ProtocolMessage protocolMessage = new ProtocolMessage();
        protocolMessage.setCheckNumber(in.readInt());
        if (protocolMessage.getCheckNumber() != ProtocolMessage.CHECK_NUMBER) {
            in.release();
            return null;
        }

        protocolMessage.setProtocol(in.readByte());
        protocolMessage.setDataLength(in.readInt());
        protocolMessage.setReserved(in.readInt());
        protocolMessage.setReserved1(in.readInt());
        protocolMessage.setNeedAck(in.readByte());
        if (in.readableBytes() < protocolMessage.getDataLength()) {
            in.release();
            return null;
        }

        if (protocolMessage.getDataLength() > 0) {
            ByteBuf buf = Unpooled.copiedBuffer(in.slice());
            protocolMessage.setData(buf);
            buf.release();
        }

        in.release();

        return protocolMessage;
    }
}