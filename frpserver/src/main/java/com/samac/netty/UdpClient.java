package com.samac.netty;

import com.samac.common.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.util.Map;

class SendData {
    public static final AttributeKey<byte[]> SOCKS5_HEADER_ATTRIBUTE_KEY = AttributeKey.valueOf("socks5-header");

    public static void send(Channel channel, byte []data) {
        PortUseBase portUseBase = channel.attr(Constants.CHILD_PORT_MAPPING_ATTRIBUTE_KEY).get();
        int index = channel.attr(Constants.CHILD_INDEX_ATTRIBUTE_KEY).get();
        if (portUseBase instanceof UdpPortMapping) {
            UdpPortMapping portMapping = (UdpPortMapping) portUseBase;
            ByteBuf byteBuf = Unpooled.copiedBuffer(data);
            ByteBuf sendBuf = Unpooled.buffer(byteBuf.readableBytes());
            byteBuf.readBytes(sendBuf);
            byteBuf.release();
            InetSocketAddress address = new InetSocketAddress(portMapping.getLocalAddress(), portMapping.getLocalPort() & 0x0FFFF);
            channel.writeAndFlush(new DatagramPacket(sendBuf, address));
        }
    }
}

class UdpClientChannelFutureListener implements ChannelFutureListener {

    private final Channel mainChannel;
    private final PortUseBase portMapping;
    private final byte[] data;
    private final int childIndex;

    public UdpClientChannelFutureListener(Channel mainChannel, PortUseBase portMapping, byte[] data, int childIndex) {
        this.mainChannel = mainChannel;
        this.portMapping = portMapping;
        this.data = data;
        this.childIndex = childIndex;
    }

    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if (channelFuture.isSuccess()) {
            Channel myChannel = channelFuture.channel();
            myChannel.attr(Constants.MAIN_CHANNEL_ATTRIBUTE_KEY).set(mainChannel);
            myChannel.attr(Constants.CHILD_PORT_MAPPING_ATTRIBUTE_KEY).set(portMapping);
            myChannel.attr(Constants.CHILD_INDEX_ATTRIBUTE_KEY).set(childIndex);
            Map<Integer, Channel> childChMap = mainChannel.attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get(portMapping.getIndex());
            childChMap.put(childIndex, myChannel);
            SendData.send(myChannel, data);
        }
    }
}

public class UdpClient {

    public static final AttributeKey<InetSocketAddress> INET_SOCKET_ADDRESS_ATTRIBUTE_KEY = AttributeKey.valueOf("inet-address");
    public static final AttributeKey<byte[]> SOCKS5_HEADER_ATTRIBUTE_KEY = AttributeKey.valueOf("socks5-header");

    Bootstrap bootstrap = new Bootstrap();

    public UdpClient(EventLoopGroup group) {
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new IdleStateHandler(0, 0, 2));
                        pipeline.addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
                                ByteBuf content = packet.content();
                                Integer childIndex = ctx.channel().attr(Constants.CHILD_INDEX_ATTRIBUTE_KEY).get();
                                PortUseBase portMapping = ctx.channel().attr(Constants.CHILD_PORT_MAPPING_ATTRIBUTE_KEY).get();
                                byte [] socksHeader = ctx.channel().attr(UdpClient.SOCKS5_HEADER_ATTRIBUTE_KEY).get();
                                ByteBuf sendBuf = null;
                                if (socksHeader != null) {
                                    sendBuf = Unpooled.buffer(socksHeader.length + content.readableBytes());
                                    sendBuf.writeBytes(socksHeader);
                                    sendBuf.writeBytes(content);
                                    //NettyBoot.getUdpServer().sendTo(new DatagramPacket(sendBuf, address));
                                } else {
                                    sendBuf = Unpooled.buffer(content.readableBytes());
                                    sendBuf.writeBytes(content);
                                    //NettyBoot.getUdpServer().sendTo(new DatagramPacket(sendBuf, address));
                                }

                                ProtocolMessage msg = new ProtocolMessage();
                                msg.setProtocol(ProtocolMessage.P_UDP_DATA_TRANSFER);
                                msg.setReserved(portMapping.getIndex());
                                msg.setReserved1(childIndex);
                                msg.setData(sendBuf);
                                sendBuf.release();
                                Channel mainChannel = ctx.channel().attr(Constants.MAIN_CHANNEL_ATTRIBUTE_KEY).get();
                                mainChannel.writeAndFlush(msg);
                            }

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                super.channelActive(ctx);
                            }

                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                Channel myChannel = ctx.channel();
                                Channel mainChannel = myChannel.attr(Constants.MAIN_CHANNEL_ATTRIBUTE_KEY).get();
                                PortUseBase portMapping = myChannel.attr(Constants.CHILD_PORT_MAPPING_ATTRIBUTE_KEY).get();
                                Integer childIndex = myChannel.attr(Constants.CHILD_INDEX_ATTRIBUTE_KEY).get();
                                Map<Integer, Channel> childChMap = mainChannel.attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get(portMapping.getIndex());
                                childChMap.remove(childIndex);
                                super.channelInactive(ctx);
                            }
                        });
                    }
                });
    }

    public void sendTo(Channel mainChannel, Channel udpChannel, PortUseBase portUseBase, ProtocolMessage msg) {
        byte[] data = new byte[msg.getData().readableBytes()];
        msg.getData().readBytes(data);
        if (udpChannel == null) {
            bootstrap.bind(0).addListener(new UdpClientChannelFutureListener(mainChannel, portUseBase, data, msg.getReserved1()));
        } else {
            SendData.send(udpChannel, data);
        }
    }

}
