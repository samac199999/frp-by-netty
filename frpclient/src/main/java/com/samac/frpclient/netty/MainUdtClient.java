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
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadFactory;

@Slf4j
public class MainUdtClient extends MainBaseClient {
    private final Bootstrap bootstrap = new Bootstrap();

    public MainUdtClient() {
        final ThreadFactory clientFactory = new DefaultThreadFactory("client");
        final NioEventLoopGroup connectGroup = new NioEventLoopGroup(0,
                clientFactory, NioUdtProvider.BYTE_PROVIDER);
        MainBaseClient mainBaseClient = this;
        bootstrap.group(connectGroup)
                .channelFactory(NioUdtProvider.BYTE_CONNECTOR)
                .handler(new ChannelInitializer<UdtChannel>() {
                    @Override
                    protected void initChannel(UdtChannel udtChannel) throws Exception {
                        ChannelPipeline pipeline = udtChannel.pipeline();
                        pipeline.addLast(new IdleCheckHandler(IdleCheckHandler.READ_IDLE_TIME, IdleCheckHandler.WRITE_IDLE_TIME, 0));

                        pipeline.addLast(new MessagePacketDecoder());
                        pipeline.addLast(new MessagePacketEncoder());

                        pipeline.addLast(new MainClientHandler(mainBaseClient));
                    }
                });
    }

    public void connect() {
        try {
            ChannelFuture channelFuture = bootstrap.connect(address, port)
                    .addListener(new MainClientChannelFutureListener(this));
//            Channel channel = channelFuture.sync().channel();
//            ChannelFuture closeFuture = channel.closeFuture();
//            log.debug("Waiting channel close");
//            closeFuture.sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
