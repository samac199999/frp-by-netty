package com.samac.frpclient.netty;

/*
 * Copyright (C) 2024 samac199999
 * All rights reserved.
 *
 * Contact: samac199999@gmail.com
 *
 * This code is licensed under the MIT License.
 * See the LICENSE file in the project root for more information.
 */

import com.samac.common.IdleCheckHandler;
import com.samac.common.MessagePacketDecoder;
import com.samac.common.MessagePacketEncoder;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainTcpClient extends MainBaseClient {

    public MainTcpClient() {
        EventLoopGroup group = new NioEventLoopGroup();
        MainBaseClient mainBaseClient = this;
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(new IdleCheckHandler(IdleCheckHandler.READ_IDLE_TIME, IdleCheckHandler.WRITE_IDLE_TIME, IdleCheckHandler.USER_CHANNEL_READ_IDLE_TIME));

                        pipeline.addLast(new MessagePacketDecoder());
                        pipeline.addLast(new MessagePacketEncoder());

                        pipeline.addLast(new MainClientHandler(mainBaseClient));
                    }
                })
                .option(ChannelOption.SO_KEEPALIVE, true);
    }
}
