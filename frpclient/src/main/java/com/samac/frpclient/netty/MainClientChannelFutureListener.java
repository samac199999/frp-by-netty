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

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MainClientChannelFutureListener implements ChannelFutureListener {
    private final MainBaseClient mainBaseClient;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    MainClientChannelFutureListener(MainBaseClient client) {
        mainBaseClient = client;
    }

    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if (channelFuture.isSuccess()) {
            log.info("Connect server success {} ", channelFuture.channel());
        } else {
            log.info("Connect server failed {} ", channelFuture.channel());
            scheduler.schedule((Runnable) mainBaseClient::connect, 5, TimeUnit.SECONDS);
        }
    }
}
