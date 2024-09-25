package com.samac.netty;

/*
 * Copyright (C) 2024 samac199999
 * All rights reserved.
 *
 * Contact: samac199999@gmail.com
 *
 * This code is licensed under the MIT License.
 * See the LICENSE file in the project root for more information.
 */

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChannelWriteListener implements ChannelFutureListener {

    private Channel channel;

    public ChannelWriteListener(Channel channel) {
        this.channel = channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void prohibitAutoRead() {
        channel.config().setOption(ChannelOption.AUTO_READ, false);
    }

    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        channel.config().setOption(ChannelOption.AUTO_READ, true);
    }
}
