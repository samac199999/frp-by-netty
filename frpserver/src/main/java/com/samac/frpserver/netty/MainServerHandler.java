package com.samac.frpserver.netty;

import com.samac.bean.Config;
import com.samac.common.*;
import com.samac.netty.*;
import com.samac.utils.RSAUtil;
import com.samac.utils.SpringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MainServerHandler extends SimpleChannelInboundHandler<ProtocolMessage> {

    static private final NioEventLoopGroup serverBossGroup = new NioEventLoopGroup();
    static private final NioEventLoopGroup serverWorkerGroup = new NioEventLoopGroup();
    static private final TcpClient tcpClient = new TcpClient(new NioEventLoopGroup());
    static private final Map<String, Channel> mainClientChannelMap = new ConcurrentHashMap<>();
    static private final UdpClient udpClient = new UdpClient(new NioEventLoopGroup());
    static private final NioEventLoopGroup udpServerGroup = new NioEventLoopGroup();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProtocolMessage msg) throws Exception {
        if (msg.getProtocol() != ProtocolMessage.P_CHECK_AUTH
        && ctx.channel().attr(Constants.CHECK_AUTH_ATTRIBUTE_KEY) == null) {
            sendErrorMessageAndCloseChannel(ctx, "Authentication information error!!!");
            msg.releaseBuf();
            return;
        }

        switch (msg.getProtocol()) {
            case ProtocolMessage.P_CHECK_AUTH:
                handleAuthMessage(ctx, msg);
                break;
            case ProtocolMessage.P_PORT_MAPPING:
                handlePortMappingMessage(ctx, msg);
                break;
            case ProtocolMessage.P_CONNECT:
            case ProtocolMessage.P_SOCKS5_CONNECT:
                handleConnectMessage(ctx, msg);
                break;
            case ProtocolMessage.P_DATA_TRANSFER:
                handleDataTransferMessage(ctx, msg);
                break;
            case ProtocolMessage.P_DISCONNECT:
                handleDisconnectMessage(ctx, msg);
                break;
            case ProtocolMessage.P_CONNECT_SUCCESS:
                handleConnectSuccessMessage(ctx, msg);
                break;
            case ProtocolMessage.P_CONNECT_FAIL:
                handleConnectFailMessage(ctx, msg);
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
            case ProtocolMessage.P_UDP_PORT_MAPPING:
                handleUdpMappingMessage(ctx, msg);
                break;
            case ProtocolMessage.P_ENCRYPT_KEY:
                handleEncryptKeyMessage(ctx, msg);
                break;
            case ProtocolMessage.P_HEART_BEAT:
                handleHeartBeatMessage(ctx, msg);
                break;
            default:
                sendErrorMessageAndCloseChannel(ctx, "Unknown protocol!!!");
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
        ByteBuf byteBuf = msg.getData();
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        try {
            bytes = RSAUtil.decryptByPrivateKey(bytes, GlobalDataManager.getRsaPrivateKey());
            String key = new String(bytes, CharsetUtil.UTF_8);
            ctx.channel().attr(Constants.ENCRYPT_KEY_ATTRIBUTE_KEY).set(key);
            msg.releaseBuf();

            ProtocolMessage rep = new ProtocolMessage();
            rep.setProtocol(ProtocolMessage.P_ENCRYPT_KEY);
            ctx.channel().writeAndFlush(rep);
        } catch (Exception e) {
            log.error("{} decrypt key error", ctx.channel());
            ctx.channel().close();
        }
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

    private void sendErrorMessageAndCloseChannel(ChannelHandlerContext ctx, String errorMsg) {
        Channel channel = ctx.channel();
        ProtocolMessage rep = new ProtocolMessage();
        rep.setProtocol(ProtocolMessage.P_SERVER_ERROR_MSG);
        ByteBuf byteBuf = Unpooled.copiedBuffer(errorMsg.getBytes());
        rep.setData(byteBuf);
        byteBuf.release();
        channel.writeAndFlush(rep);
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    private void handleAuthMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        String authStr = msg.getData().toString(CharsetUtil.UTF_8);
        msg.releaseBuf();
        Channel channel = ctx.channel();
        String[] auths = authStr.split("##SAMAC##");
        Config config = SpringUtil.getBean("config");
        if (auths.length != 2 || !auths[1].equals(config.getPassword())) {
            sendErrorMessageAndCloseChannel(ctx, "Authentication information error!!!");
            return;
        }

        String token = auths[0];
        if (mainClientChannelMap.containsKey(token)) {
            sendErrorMessageAndCloseChannel(ctx, "Have one channel alive!!!");
            return;
        }

        ProtocolMessage rep = new ProtocolMessage();
        rep.setProtocol(ProtocolMessage.P_CHECK_AUTH);
        ByteBuf byteBuf = Unpooled.copiedBuffer(GlobalDataManager.getRsaPublicKey().getBytes(CharsetUtil.UTF_8));
        rep.setData(byteBuf);
        byteBuf.release();
        channel.writeAndFlush(rep);

        channel.attr(Constants.CHECK_AUTH_ATTRIBUTE_KEY).set(token);
        mainClientChannelMap.put(token, channel);
    }

    private void handlePortMappingMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        PortMapping portMapping = new PortMapping();
        portMapping.setBytes(msg.getData());
        msg.releaseBuf();
        Map<Short, PortUseBase> portMappingMap = ctx.channel().attr(Constants.PORT_MAPPING_ATTRIBUTE_KEY).get();
        ctx.channel().attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().put(portMapping.getIndex(), new ConcurrentHashMap<>());
        portMappingMap.put(portMapping.getIndex(), portMapping);
        TcpServer tcpServer = ctx.channel().attr(Constants.TCP_SERVER_ATTRIBUTE_KEY).get();
        if (!portMapping.isFlip()) {
            tcpServer.bind(ctx.channel(), portMapping);
        }
    }

    private void handleUdpMappingMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        UdpPortMapping portMapping = new UdpPortMapping();
        portMapping.setBytes(msg.getData());
        msg.releaseBuf();
        Map<Short, PortUseBase> portMappingMap = ctx.channel().attr(Constants.PORT_MAPPING_ATTRIBUTE_KEY).get();
        ctx.channel().attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().put(portMapping.getIndex(), new ConcurrentHashMap<>());
        portMappingMap.put(portMapping.getIndex(), portMapping);
        UdpServer udpServer = ctx.channel().attr(Constants.UDP_SERVER_ATTRIBUTE_KEY).get();
        if (!portMapping.isFlip()) {
            udpServer.bind(ctx.channel(), portMapping);
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

    private void handleDataTransferMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        log.debug("handleDataTransferMessage {}", ctx.channel());
        Channel childChannel = ctx.channel().attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get((short)msg.getReserved()).get(msg.getReserved1());
        if (childChannel != null) {
        //    ChannelWriteListener channelWriteListener = ctx.channel().attr(Constants.CHANNEL_WRITE_LISTENER_ATTRIBUTE_KEY).get();
        //    channelWriteListener.prohibitAutoRead();
            childChannel.writeAndFlush(msg.getData()); //.addListener(channelWriteListener);
        }
    }

    private void handleUdpDataTransferMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        log.debug("handleUdpDataTransferMessage {}", ctx.channel());
        Map<Short, PortUseBase> portMappingMap = ctx.channel().attr(Constants.PORT_MAPPING_ATTRIBUTE_KEY).get();
        PortUseBase portMapping = portMappingMap.get((short)msg.getReserved());
        if (portMapping.isFlip()) {
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
            Channel childChannel = ctx.channel().attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get((short)msg.getReserved()).get(0);
            UdpServer udpServer = ctx.channel().attr(Constants.UDP_SERVER_ATTRIBUTE_KEY).get();
            udpServer.sendTo(childChannel, msg);
        }
    }

    private void handleTransDataAck(ChannelHandlerContext ctx, ProtocolMessage msg) {
        //log.info("handleTransDataAck {}", ctx.channel());
        Channel childChannel = ctx.channel().attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get((short)msg.getReserved()).get(msg.getReserved1());
        if (childChannel != null) {
            childChannel.config().setOption(ChannelOption.AUTO_READ, true);
        }
    }

    private void handleDisconnectMessage(ChannelHandlerContext ctx, ProtocolMessage msg) {
        log.debug("handleDisconnectMessage {}", ctx.channel());
        Channel childChannel = ctx.channel().attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get((short)msg.getReserved()).get(msg.getReserved1());
        if (childChannel != null && childChannel.isOpen() && childChannel.isActive()) {
            childChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
        msg.releaseBuf();
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
        //log.info("handleConnectFailMessage {}", ctx.channel());
        Channel childChannel = ctx.channel().attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get((short)msg.getReserved()).get(msg.getReserved1());
        if (childChannel != null) {
            childChannel.close();
        }
        msg.releaseBuf();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Client channel {}", ctx.channel());
        ctx.channel().attr(Constants.PORT_MAPPING_ATTRIBUTE_KEY).set(new ConcurrentHashMap<>());
        ctx.channel().attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).set(new ConcurrentHashMap<>());
        ctx.channel().attr(Constants.ATOMIC_INTEGER_ATTRIBUTE_KEY).set(new AtomicInteger(0));
        TcpServer tcpServer = new TcpServer(serverBossGroup, serverWorkerGroup);
        UdpServer udpServer = new UdpServer(udpServerGroup);
        ctx.channel().attr(Constants.TCP_SERVER_ATTRIBUTE_KEY).set(tcpServer);
        ctx.channel().attr(Constants.UDP_SERVER_ATTRIBUTE_KEY).set(udpServer);
        ctx.channel().attr(Constants.CHANNEL_WRITE_LISTENER_ATTRIBUTE_KEY).set(new ChannelWriteListener(ctx.channel()));
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

        String token = ctx.channel().attr(Constants.CHECK_AUTH_ATTRIBUTE_KEY).get();
        mainClientChannelMap.remove(token);

        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (ctx == null || cause == null) {
            return; // 防止空指针异常
        }

        log.error("MainServerHandler Channel error: {} {}", ctx.channel(), cause.getMessage());
        ctx.channel().close();
    //    super.exceptionCaught(ctx, cause);
    }
}
