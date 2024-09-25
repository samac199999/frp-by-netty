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

import com.samac.bean.Config;
import com.samac.common.*;
import com.samac.utils.SpringUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TcpServer {

    private final ServerBootstrap bootstrap;

    private Channel mainChannel = null;
    private final Map<Integer, PortUseBase> portMappingMap = new ConcurrentHashMap<>();
    private final List<Channel> serverChannels = new ArrayList<>();

    public TcpServer(NioEventLoopGroup serverBossGroup, NioEventLoopGroup serverWorkerGroup) {
        bootstrap = new ServerBootstrap();
        bootstrap.group(serverBossGroup, serverWorkerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                Config config = (Config) SpringUtil.getBean("config");
                ch.pipeline().addLast(new ChannelTrafficShapingHandler(config.getWriteLimit(), config.getReadLimit(), 1000));
                ch.pipeline().addLast(new IdleStateHandler(45, 60, 120, TimeUnit.SECONDS));
                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object o) throws Exception {
                        Channel myChannel = ctx.channel();
                        Channel mainChannel = myChannel.attr(Constants.MAIN_CHANNEL_ATTRIBUTE_KEY).get();
                        PortUseBase portMapping = myChannel.attr(Constants.CHILD_PORT_MAPPING_ATTRIBUTE_KEY).get();
                        Integer childIndex = myChannel.attr(Constants.CHILD_INDEX_ATTRIBUTE_KEY).get();

                        ByteBuf in = (ByteBuf)o;
                        ChannelWriteListener channelWriteListener = myChannel.attr(Constants.CHANNEL_WRITE_LISTENER_ATTRIBUTE_KEY).get();
                        channelWriteListener.prohibitAutoRead();

                        if (portMapping instanceof PortMapping) {
                            if (mainChannel.isOpen() && mainChannel.isActive()) {
                                ProtocolMessage msg = new ProtocolMessage();
                                msg.setProtocol(ProtocolMessage.P_DATA_TRANSFER);
                                msg.setReserved(portMapping.getIndex());
                                msg.setReserved1(childIndex);
                                msg.setData(in);
                                mainChannel.writeAndFlush(msg).addListener(channelWriteListener);
                            }
                        }

                        in.release();
                    }

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        log.debug("channel active :", ctx.channel());
                        Channel myChannel = ctx.channel();
                        InetSocketAddress sa = (InetSocketAddress) myChannel.localAddress();
                        PortUseBase portMapping = portMappingMap.get(sa.getPort());

                        ctx.channel().attr(Constants.CHANNEL_WRITE_LISTENER_ATTRIBUTE_KEY).set(new ChannelWriteListener(myChannel));

                        int index = mainChannel.attr(Constants.ATOMIC_INTEGER_ATTRIBUTE_KEY).get().incrementAndGet();
                        myChannel.attr(Constants.MAIN_CHANNEL_ATTRIBUTE_KEY).set(mainChannel);
                        myChannel.attr(Constants.CHILD_INDEX_ATTRIBUTE_KEY).set(index);
                        mainChannel.attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get(portMapping.getIndex()).put(index, myChannel);
                        myChannel.attr(Constants.CHILD_PORT_MAPPING_ATTRIBUTE_KEY).set(portMapping);
                        if (portMapping instanceof PortMapping) {
                            myChannel.config().setOption(ChannelOption.AUTO_READ, false);

                            if (mainChannel.isOpen() && myChannel.isActive()) {
                                ProtocolMessage msg = new ProtocolMessage();
                                msg.setProtocol(ProtocolMessage.P_CONNECT);
                                msg.setReserved(portMapping.getIndex());
                                msg.setReserved1(index);
                                mainChannel.writeAndFlush(msg);
                            }
                        }

                        super.channelActive(ctx);
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        Channel myChannel = ctx.channel();
                        Channel mainChannel = myChannel.attr(Constants.MAIN_CHANNEL_ATTRIBUTE_KEY).get();
                        PortUseBase portMapping = myChannel.attr(Constants.CHILD_PORT_MAPPING_ATTRIBUTE_KEY).get();
                        Integer childIndex = myChannel.attr(Constants.CHILD_INDEX_ATTRIBUTE_KEY).get();

                        if (mainChannel.isOpen() && mainChannel.isActive()) {
                            ProtocolMessage msg = new ProtocolMessage();
                            msg.setProtocol(ProtocolMessage.P_DISCONNECT);
                            msg.setReserved(portMapping.getIndex());
                            msg.setReserved1(childIndex);
                            mainChannel.writeAndFlush(msg);
                            Map<Integer, Channel> childChMap = mainChannel.attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get(portMapping.getIndex());
                            childChMap.remove(childIndex);
                        }

                        super.channelInactive(ctx);
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        log.error("TcpServerHandler Channel error: {} {}", ctx.channel(), cause.toString());
                        ctx.channel().close();
                    //    super.exceptionCaught(ctx, cause);
                    }
                });
            }
        });
    }

    public void bind(Channel mainChannel, PortUseBase portMapping) {
        if (portMappingMap.get(portMapping.getPort()) != null) {
            log.warn("port has bind {}", portMapping.getPort());
            return;
        }

        if (this.mainChannel == null || !this.mainChannel.isOpen() || !this.mainChannel.isActive()) {
            this.mainChannel = mainChannel;
        }

        portMappingMap.put(portMapping.getPort(), portMapping);
        try {
            ChannelFuture channelFuture = bootstrap.bind("0.0.0.0", portMapping.getPort()).sync();
            log.info("Bind port: {}", portMapping.getPort());
            serverChannels.add(channelFuture.channel());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {

        for (Channel ch : serverChannels
             ) {
            try {
                ch.close().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        serverChannels.clear();
        portMappingMap.clear();
    }
}
