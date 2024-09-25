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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class MainBaseClient {
    protected String address;
    protected int port;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    protected final Bootstrap bootstrap = new Bootstrap();

    public void connect(String address, int port) {
        this.address = address;
        this.port = port;
        connect();
    }

    public void connect() {
        try {
            ChannelFuture channelFuture = bootstrap.connect(address, port)
                    .addListener(new MainClientChannelFutureListener(this));
//            Channel channel = channelFuture.sync().channel();
//            ChannelFuture closeFuture = channel.closeFuture();
//            closeFuture.sync();
        } catch (Exception e) {
            log.error("Connect server failed...");
            scheduler.schedule((Runnable) this::connect, 5, TimeUnit.SECONDS);
        }
    }
}
