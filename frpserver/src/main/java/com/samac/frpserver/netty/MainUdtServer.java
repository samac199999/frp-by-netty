package com.samac.frpserver.netty;

import com.samac.common.IdleCheckHandler;
import com.samac.common.MessagePacketDecoder;
import com.samac.common.MessagePacketEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadFactory;

@Slf4j
public class MainUdtServer {
    private String host = "localhost";
    //端口号
    private int port = 8080;

    public void init() throws Exception {
        final ThreadFactory acceptFactory = new DefaultThreadFactory("accept");
        final ThreadFactory connectFactory = new DefaultThreadFactory("connect");
        final NioEventLoopGroup acceptGroup = new NioEventLoopGroup(1, acceptFactory, NioUdtProvider.BYTE_PROVIDER);
        final NioEventLoopGroup connectGroup = new NioEventLoopGroup(0, connectFactory, NioUdtProvider.BYTE_PROVIDER);

        final ServerBootstrap boot = new ServerBootstrap();
        boot.group(acceptGroup, connectGroup)
                .channelFactory(NioUdtProvider.BYTE_ACCEPTOR)
                .option(ChannelOption.SO_BACKLOG, 10)
                .childHandler(new ChannelInitializer<UdtChannel>() {
                    @Override
                    protected void initChannel(UdtChannel udtChannel) throws Exception {
                        ChannelPipeline pipeline = udtChannel.pipeline();
                        pipeline.addLast(new IdleCheckHandler(IdleCheckHandler.READ_IDLE_TIME, IdleCheckHandler.WRITE_IDLE_TIME, 0));

                        //自定义编解码器
                        pipeline.addLast(new MessagePacketDecoder());
                        pipeline.addLast(new MessagePacketEncoder());

                        //自定义Hadler
                        pipeline.addLast(new MainServerHandler());
                    }
                });

        // 开启服务
        final ChannelFuture future = boot.bind(host, port).sync();
        if(future.isSuccess()){
            log.info("FRP SERVER UDT START---------------");
        } else {
            log.info("FRP SERVER UDT FAILED---------------");
        }
    }

    /**
     * 服务启动
     */
    public synchronized void startServer(String host, int port) {
        try {
            this.host = host;
            this.port = port;
            this.init();
        }catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
