package com.samac.netty;

import com.samac.bean.Config;
import com.samac.common.*;
import com.samac.utils.SpringUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TcpClient {
    private final Bootstrap bootstrap = new Bootstrap();

    public TcpClient(EventLoopGroup group) {
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        Config config = (Config) SpringUtil.getBean("config");
                        pipeline.addLast(new ChannelTrafficShapingHandler(config.getWriteLimit(), config.getReadLimit(), 1000));
                        pipeline.addLast(new IdleStateHandler(45, 60, 120, TimeUnit.SECONDS));

                        //自定义编解码器
                        //pipeline.addLast(new MessagePacketDecoder());
                        //pipeline.addLast(new MessagePacketEncoder());

                        //自定义Handler
                        pipeline.addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object o) throws Exception {
                                Channel myChannel = ctx.channel();
                                Channel mainChannel = myChannel.attr(Constants.MAIN_CHANNEL_ATTRIBUTE_KEY).get();
                                PortUseBase portMapping = myChannel.attr(Constants.CHILD_PORT_MAPPING_ATTRIBUTE_KEY).get();
                                Integer childIndex = myChannel.attr(Constants.CHILD_INDEX_ATTRIBUTE_KEY).get();

                                ByteBuf in = (ByteBuf)o;
                                ChannelWriteListener channelWriteListener = myChannel.attr(Constants.CHANNEL_WRITE_LISTENER_ATTRIBUTE_KEY).get();
                                channelWriteListener.prohibitAutoRead();

                                if (mainChannel.isOpen() && mainChannel.isActive()) {
                                    ProtocolMessage msg = new ProtocolMessage();
                                    msg.setProtocol(ProtocolMessage.P_DATA_TRANSFER);
                                    msg.setReserved(portMapping.getIndex());
                                    msg.setReserved1(childIndex);
                                    msg.setData(in);
                                    mainChannel.writeAndFlush(msg).addListener(channelWriteListener);
                                } else {
                                    myChannel.close();
                                }

                                in.release();
                            }

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                ctx.channel().attr(Constants.CHANNEL_WRITE_LISTENER_ATTRIBUTE_KEY).set(new ChannelWriteListener(ctx.channel()));
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
                                log.error("TcpClientHandler Channel error: {} {}", ctx.channel(), cause.toString());
                                ctx.channel().close();
                            //    super.exceptionCaught(ctx, cause);
                            }
                        });
                    }
                })
                .option(ChannelOption.SO_KEEPALIVE, true);
    }

    public void connect(Channel mainChannel, PortMapping portMapping, int childIndex) {
        bootstrap.connect(portMapping.getLocalAddress(), portMapping.getLocalPort())
                .addListener(new TcpClientChannelFutureListener(mainChannel, portMapping, childIndex));
    }
}
