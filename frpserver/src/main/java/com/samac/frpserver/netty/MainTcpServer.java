package com.samac.frpserver.netty;

import com.samac.bean.Config;
import com.samac.common.IdleCheckHandler;
import com.samac.common.MessagePacketDecoder;
import com.samac.common.MessagePacketEncoder;
import com.samac.utils.SpringUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainTcpServer {
    private String host = "localhost";
    //端口号
    private int port = 8080;
    //服务器运行状态
    private volatile boolean isRunning = false;
    //处理Accept连接事件的线程，这里线程数设置为1即可，netty处理链接事件默认为单线程，过度设置反而浪费cpu资源
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    //处理handler的工作线程，其实也就是处理IO读写 。线程数据默认为 CPU 核心数乘以2
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    public void init() throws Exception{
        //创建ServerBootstrap实例
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        //初始化ServerBootstrap的线程组
        serverBootstrap.group(bossGroup, workerGroup);//
        //设置将要被实例化的ServerChannel类
        serverBootstrap.channel(NioServerSocketChannel.class);//
        //在ServerChannelInitializer中初始化ChannelPipeline责任链，并添加到serverBootstrap中
        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline pipeline = socketChannel.pipeline();
                Config config = (Config) SpringUtil.getBean("config");
                pipeline.addLast(new ChannelTrafficShapingHandler(config.getWriteLimit(), config.getReadLimit(), 1000));
                pipeline.addLast(new IdleCheckHandler(IdleCheckHandler.READ_IDLE_TIME, IdleCheckHandler.WRITE_IDLE_TIME, IdleCheckHandler.USER_CHANNEL_READ_IDLE_TIME));

                //自定义编解码器
                pipeline.addLast(new MessagePacketDecoder());
                pipeline.addLast(new MessagePacketEncoder());

                //自定义Handler
                pipeline.addLast(new MainServerHandler());
            }
        });
        //标识当服务器请求处理线程全满时，用于临时存放已完成三次握手的请求的队列的最大长度
        serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);
        // 是否启用心跳保活机机制
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
        //绑定端口后，开启监听
        ChannelFuture channelFuture = serverBootstrap.bind(host, port).sync();
        if(channelFuture.isSuccess()){
            log.info("FRP SERVER TCP Bind: {} ---------------", port);
        } else {
            log.info("FRP SERVER TCP Bind error ---------------");
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

    /**
     * 服务关闭
     */
    public synchronized void stopServer() {
        if (!this.isRunning) {
            throw new IllegalStateException(this.getName() + " 未启动 .");
        }
        this.isRunning = false;
        try {
            Future<?> future = this.workerGroup.shutdownGracefully().await();
            if (!future.isSuccess()) {
                log.error("workerGroup:{}", future.cause());
            }

            future = this.bossGroup.shutdownGracefully().await();
            if (!future.isSuccess()) {
                log.error("bossGroup:{}", future.cause());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("TCP Service start...");
    }

    private String getName() {
        return "TCP-Server";
    }
}
