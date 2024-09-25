package com.samac.frpserver.netty;

import com.samac.bean.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NettyBoot implements ApplicationListener<ContextRefreshedEvent> {

    private static MainTcpServer tcpServer = null;
    private static MainUdtServer udtServer = null;

    private Config config;
    @Autowired
    public void setConfig(Config config) {
        this.config = config;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (contextRefreshedEvent.getApplicationContext().getParent() == null) {
            try{
                if (tcpServer == null) {
                    tcpServer = new MainTcpServer();
                    tcpServer.startServer(config.getHost(), config.getPort());
                }

                if (config.isUseUdt() && udtServer == null) {
                    udtServer = new MainUdtServer();
                    udtServer.startServer(config.getHost(), config.getPort());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            log.info("Main Server Started...");
        }
    }
}
