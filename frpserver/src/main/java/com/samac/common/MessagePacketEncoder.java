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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessagePacketEncoder extends MessageToByteEncoder<ProtocolMessage>
{

    @Override
    protected void encode(ChannelHandlerContext ctx, ProtocolMessage msg, ByteBuf out) throws Exception {
        out.writeInt(msg.getCheckNumber());
        out.writeByte(msg.getProtocol());
        out.writeInt(msg.getDataLength());
        out.writeInt(msg.getReserved());
        out.writeInt(msg.getReserved1());
        out.writeByte(msg.getNeedAck());
        if (msg.getData() != null) {
            out.writeBytes(msg.getData());
            msg.releaseBuf();
        }
    }
}

