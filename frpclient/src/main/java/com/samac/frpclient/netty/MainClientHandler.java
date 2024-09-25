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

import com.samac.bean.Config;
import com.samac.common.*;
import com.samac.netty.*;
import com.samac.utils.SpringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MainClientHandler extends SimpleChannelInboundHandler<ProtocolMessage> {

    static private final NioEventLoopGroup serverBossGroup = new NioEventLoopGroup();
    static private final NioEventLoopGroup serverWorkerGroup = new NioEventLoopGroup();
//    static private TcpServer tcpServer = null;
    static private final TcpClient tcpClient = new TcpClient(new NioEventLoopGroup());
//    static private UdpServer udpServer = null;
    static private final UdpClient udpClient = new UdpClient(new NioEventLoopGroup());
    static private final NioEventLoopGroup udpServerGroup = new NioEventLoopGroup();

    private final MainBaseClient mainBaseClient;

    private boolean exceptionCaught = false;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    MainClientHandler(MainBaseClient client) {
        mainBaseClient = client;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProtocolMessage msg) throws Exception {
        switch (msg.getProtocol()) {
            case ProtocolMessage.P_CHECK_AUTH:
                handleAuthMessage(ctx, msg);
                break;
            case ProtocolMessage.P_CONNECT_SUCCESS:
                handleConnectSuccessMessage(ctx, msg);
                break;
            case ProtocolMessage.P_CONNECT:
            case ProtocolMessage.P_SOCKS5_CONNECT:
                handleConnectMessage(ctx, msg);
                break;
            case ProtocolMessage.P_DATA_TRANSFER:
                handleDataTransferMessage(ctx, msg);
                break;
            case ProtocolMessage.P_CONNECT_FAIL:
                handleConnectFailMessage(ctx, msg);
                break;
            case ProtocolMessage.P_DISCONNECT:
                handleDisconnectMessage(ctx, msg);
                break;
            case ProtocolMessage.P_SERVER_ERROR_MSG:
                handleServerErrorMessage(ctx, msg);
                break;
            case ProtocolMessage.P_TEST_MSG:
                handleTestMessage(ctx, msg);
                break;
            case ProtocolMessage.P_TRANS_DATA_ACK:
                handleTransDataAck(ctx, msg);
                break;
            case ProtocolMessage.P_UDP_DATA_TRANSFER:
                handleUdpDataTransferMessage(ctx, msg);
                break;
            case ProtocolMessage.P_ENCRYPT_KEY:
                handleEncryptKeyMessage(ctx, msg);
                break;
            case ProtocolMessage.P_HEART_BEAT:
                handleHeartBeatMessage(ctx, msg);
                break;
            default:
                msg.releaseBuf();
                break;
        }
    }

    private void handleHeartBeatMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        msg.releaseBuf();
        Channel channel = ctx.channel();
        ProtocolMessage rep = new ProtocolMessage();
        rep.setProtocol(ProtocolMessage.P_HEART_BEAT_ACK);
        channel.writeAndFlush(rep);
        log.info("Read a heart beat message @ {}", channel);
    }

    private void handleEncryptKeyMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        msg.releaseBuf();
        sendPortMappings(ctx.channel());
    }

    private void handleTestMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        if (msg == null) {
            msg = new ProtocolMessage();
            msg.setProtocol(ProtocolMessage.P_TEST_MSG);
            ByteBuf byteBuf = Unpooled.copiedBuffer("This is a test message".getBytes());
            msg.setData(byteBuf);
            byteBuf.release();
        }
        log.debug(msg.getData().toString(CharsetUtil.UTF_8));
        ctx.channel().writeAndFlush(msg);
    }

    private void handleAuthMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        log.debug("handleAuthMessage {}", ctx.channel());
        ByteBuf byteBuf = msg.getData();
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        msg.releaseBuf();
        sendPortMappings(ctx.channel());
    }

    private void handleConnectSuccessMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        log.debug("handleConnectSuccessMessage {}", ctx.channel());
        Channel childChannel = ctx.channel().attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get((short)msg.getReserved()).get(msg.getReserved1());
        if (childChannel != null) {
            childChannel.config().setOption(ChannelOption.AUTO_READ, true);
        }
        msg.releaseBuf();
    }

    private void handleConnectFailMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        log.debug("handleConnectFailMessage {}", ctx.channel());
        Channel childChannel = ctx.channel().attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get((short)msg.getReserved()).get(msg.getReserved1());
        if (childChannel != null) {
            childChannel.close();
        }
        msg.releaseBuf();
    }

    private void handleDisconnectMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        log.debug("handleDisconnectMessage {}", ctx.channel());
        Channel childChannel = ctx.channel().attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get((short)msg.getReserved()).get(msg.getReserved1());
        if (childChannel != null && childChannel.isOpen() && childChannel.isActive()) {
            childChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }

        msg.releaseBuf();
    }

    private void handleDataTransferMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        log.debug("handleDataTransferMessage {}", ctx.channel());
        Channel childChannel = ctx.channel().attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get((short)msg.getReserved()).get(msg.getReserved1());
        if (childChannel != null) {
            childChannel.writeAndFlush(msg.getData());
        }
    }

    private void handleUdpDataTransferMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        Channel channel = ctx.channel();
        Map<Short, PortUseBase> portMappingMap = ctx.channel().attr(Constants.PORT_MAPPING_ATTRIBUTE_KEY).get();
        PortUseBase portMapping = portMappingMap.get((short)msg.getReserved());
        if (!portMapping.isFlip()) {
            Channel childChannel = ctx.channel().attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get((short)msg.getReserved()).get(msg.getReserved1());
            if (childChannel != null && childChannel.isOpen() && childChannel.isActive()) {
                udpClient.sendTo(ctx.channel(),
                        childChannel,
                        portMappingMap.get((short)msg.getReserved()),
                        msg);
            } else {
                udpClient.sendTo(ctx.channel(),
                        null,
                        portMappingMap.get((short)msg.getReserved()),
                        msg);
            }

            msg.releaseBuf();
        } else {
            Channel childChannel = channel.attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get((short)msg.getReserved()).get(0);
            UdpServer udpServer = channel.attr(Constants.UDP_SERVER_ATTRIBUTE_KEY).get();
            udpServer.sendTo(childChannel, msg);
        }
    }

    private void handleTransDataAck(ChannelHandlerContext ctx, ProtocolMessage msg) {
        log.debug("handleTransDataAck {}", ctx.channel());
        Channel childChannel = ctx.channel().attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get((short)msg.getReserved()).get(msg.getReserved1());
        if (childChannel != null) {
            childChannel.config().setOption(ChannelOption.AUTO_READ, true);
        }
    }

    private void handleConnectMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        log.debug("handleConnectMessage {}", ctx.channel());
        Map<Short, PortUseBase> portMappingMap = ctx.channel().attr(Constants.PORT_MAPPING_ATTRIBUTE_KEY).get();
        PortUseBase portMapping = portMappingMap.get((short)msg.getReserved());
        if (portMapping instanceof PortMapping)
            tcpClient.connect(ctx.channel(), (PortMapping) portMapping, msg.getReserved1());

        msg.releaseBuf();
    }

    private void handleServerErrorMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        String message = msg.getData().toString(CharsetUtil.UTF_8);
        msg.releaseBuf();
        log.error(message);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        log.debug("Client channel {}", channel);

        channel.attr(Constants.PORT_MAPPING_ATTRIBUTE_KEY).set(new ConcurrentHashMap<>());
        channel.attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).set(new ConcurrentHashMap<>());
        channel.attr(Constants.ATOMIC_INTEGER_ATTRIBUTE_KEY).set(new AtomicInteger(0));
        ctx.channel().attr(Constants.CHANNEL_WRITE_LISTENER_ATTRIBUTE_KEY).set(new ChannelWriteListener(ctx.channel()));

        ProtocolMessage msg = new ProtocolMessage();
        msg.setProtocol(ProtocolMessage.P_CHECK_AUTH);
        Config config = SpringUtil.getBean("config");
        String authString = config.getToken() + "##SAMAC##" + config.getPassword();
        ByteBuf byteBuf = Unpooled.copiedBuffer(authString.getBytes());
        msg.setData(byteBuf);
        byteBuf.release();
        channel.writeAndFlush(msg);

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Map<Short, Map<Integer, Channel>> childChannelMap = ctx.channel().attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get();
        for (Map.Entry<Short, Map<Integer, Channel>> entry : childChannelMap.entrySet()
        ) {
            Map<Integer, Channel> channelMap = entry.getValue();
            for (Map.Entry<Integer, Channel> entry2 : channelMap.entrySet()
            ) {
                Channel childChannel = entry2.getValue();
                if (childChannel != null && childChannel.isOpen() && childChannel.isActive()) {
                    childChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(
                            ChannelFutureListener.CLOSE);
                }
            }
        }

        ctx.channel().attr(Constants.TCP_SERVER_ATTRIBUTE_KEY).get().stop();
        ctx.channel().attr(Constants.UDP_SERVER_ATTRIBUTE_KEY).get().stop();

        super.channelInactive(ctx);

        if (!exceptionCaught) {
            exceptionCaught = true;
            scheduleReconnect();
        }
    }

    private void sendPortMappings(Channel channel) {
        TcpServer tcpServer = new TcpServer(serverBossGroup, serverWorkerGroup);
        UdpServer udpServer = new UdpServer(udpServerGroup);
        channel.attr(Constants.TCP_SERVER_ATTRIBUTE_KEY).set(tcpServer);
        channel.attr(Constants.UDP_SERVER_ATTRIBUTE_KEY).set(udpServer);
        List<PortUseBase> mappings = GlobalDataManager.mappingList;
        Map<Short, PortUseBase> portMappingMap = channel.attr(Constants.PORT_MAPPING_ATTRIBUTE_KEY).get();
        Map<Short, Map<Integer, Channel>> childChannelMap = channel.attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get();
        for (PortUseBase portUseBase : mappings) {
            portMappingMap.put(portUseBase.getIndex(), portUseBase);
            childChannelMap.put(portUseBase.getIndex(), new ConcurrentHashMap<>());
            if (portUseBase instanceof PortMapping) {
                PortMapping m = (PortMapping) portUseBase;
                ProtocolMessage msg = new ProtocolMessage();
                msg.setProtocol(ProtocolMessage.P_PORT_MAPPING);
                ByteBuf buf = m.getBytes();
                msg.setData(buf);
                buf.release();
                channel.writeAndFlush(msg);
            } else if (portUseBase instanceof UdpPortMapping) {
                UdpPortMapping m = (UdpPortMapping) portUseBase;
                ProtocolMessage msg = new ProtocolMessage();
                msg.setProtocol(ProtocolMessage.P_UDP_PORT_MAPPING);
                ByteBuf buf = m.getBytes();
                msg.setData(buf);
                buf.release();
                channel.writeAndFlush(msg);
            }

            if (portUseBase.isFlip()) {
                if (portUseBase instanceof PortMapping) {
                    tcpServer.bind(channel, portUseBase);
                } else if (portUseBase instanceof UdpPortMapping) {
                    udpServer.bind(channel, portUseBase);
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("MainClientHandler Channel error: {} {}", ctx.channel(), cause.toString());
        ctx.channel().close();
        if (!exceptionCaught) {
            exceptionCaught = true;
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        scheduler.schedule((Runnable) mainBaseClient::connect, 5, TimeUnit.SECONDS);
    }
}
