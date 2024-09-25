package com.samac.netty;

import com.samac.common.Constants;
import com.samac.common.PortUseBase;
import com.samac.common.ProtocolMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class UdpServer {
    Bootstrap bootstrap = new Bootstrap();

    private Channel mainChannel = null;
    private final Map<Integer, PortUseBase> portMappingMap = new ConcurrentHashMap<>();
    private final List<Channel> serverChannels = new ArrayList<>();
    private final Map<Integer, InetSocketAddress> senderMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> senderMap2 = new ConcurrentHashMap<>();
    private final AtomicInteger senderCount = new AtomicInteger(0);

    public UdpServer(EventLoopGroup group) {
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .option(ChannelOption.SO_RCVBUF, 2048 * 1024)
                .option(ChannelOption.SO_SNDBUF, 2048 * 1024)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel nioDatagramChannel) throws Exception {
                        ChannelPipeline pipeline = nioDatagramChannel.pipeline();
                        pipeline.addLast(new NioEventLoopGroup(), new SimpleChannelInboundHandler<DatagramPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
                                Channel myChannel = ctx.channel();
                                Channel mainChannel = myChannel.attr(Constants.MAIN_CHANNEL_ATTRIBUTE_KEY).get();
                                PortUseBase portMapping = myChannel.attr(Constants.CHILD_PORT_MAPPING_ATTRIBUTE_KEY).get();
                                //Integer childIndex = myChannel.attr(Constants.CHILD_INDEX_ATTRIBUTE_KEY).get();
                                //ChannelWriteListener channelWriteListener = myChannel.attr(Constants.CHANNEL_WRITE_LISTENER_ATTRIBUTE_KEY).get();
                                //channelWriteListener.prohibitAutoRead();

                                String senderStr = packet.sender().getHostString() + ":" + packet.sender().getPort();
                                if (!senderMap2.containsKey(senderStr)) {
                                    int key = senderCount.incrementAndGet();
                                    senderMap.put(key, packet.sender());
                                    senderMap2.put(senderStr, key);
                                }

                                ProtocolMessage msg = new ProtocolMessage();
                                msg.setProtocol(ProtocolMessage.P_UDP_DATA_TRANSFER);
                                msg.setReserved(portMapping.getIndex());
                                msg.setReserved1(senderMap2.get(senderStr));
                                msg.setData(packet.content());
                                //packet.release();
                                //log.info("main ch {} {} {}", mainChannel, mainChannel.isOpen(), mainChannel.isActive());
                                mainChannel.writeAndFlush(msg); //.addListener(channelWriteListener);
                            }

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                log.debug("channel active :", ctx.channel());
                                Channel myChannel = ctx.channel();
                                InetSocketAddress sa = (InetSocketAddress) myChannel.localAddress();
                                PortUseBase portMapping = portMappingMap.get(sa.getPort());

                                ctx.channel().attr(Constants.CHANNEL_WRITE_LISTENER_ATTRIBUTE_KEY).set(new ChannelWriteListener(myChannel));

                            //    int index = mainChannel.attr(Constants.ATOMIC_INTEGER_ATTRIBUTE_KEY).get().incrementAndGet();
                                myChannel.attr(Constants.MAIN_CHANNEL_ATTRIBUTE_KEY).set(mainChannel);
                            //    myChannel.attr(Constants.CHILD_INDEX_ATTRIBUTE_KEY).set(index);
                                mainChannel.attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get(portMapping.getIndex()).put(0, myChannel);
                                myChannel.attr(Constants.CHILD_PORT_MAPPING_ATTRIBUTE_KEY).set(portMapping);
                                myChannel.attr(Constants.CHANNEL_WRITE_LISTENER_ATTRIBUTE_KEY).set(new ChannelWriteListener(myChannel));

                                super.channelActive(ctx);
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

    public void sendTo(Channel channel, ProtocolMessage msg) {
        InetSocketAddress address = senderMap.get(msg.getReserved1());
        channel.writeAndFlush(new DatagramPacket(msg.getData(), address));
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
