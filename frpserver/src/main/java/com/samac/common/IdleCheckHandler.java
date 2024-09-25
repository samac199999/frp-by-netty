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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class IdleCheckHandler extends IdleStateHandler {

    public static final int USER_CHANNEL_READ_IDLE_TIME = 120;
    public static final int READ_IDLE_TIME = 60;
    public static final int WRITE_IDLE_TIME = 45;

    public IdleCheckHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
        super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds, TimeUnit.SECONDS);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {

        if (IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT == evt) {
            ProtocolMessage msg = new ProtocolMessage();
            msg.setProtocol(ProtocolMessage.P_HEART_BEAT);
            ctx.channel().writeAndFlush(msg);
            log.info("channel send heart beat... {}", ctx.channel());
        } else if (IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT == evt) {
            log.error("channel read timeout {}", ctx.channel());
            ctx.channel().close();
        }

        super.channelIdle(ctx, evt);
    }
}
