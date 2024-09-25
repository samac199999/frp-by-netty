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
import com.samac.common.GlobalDataManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NettyBoot implements ApplicationListener<ContextRefreshedEvent> {

    private static MainTcpClient mainTcpClient = null;
    private static MainUdtClient mainUdtClient = null;

    private Config config;

    @Autowired
    public void setConfig(Config config) {
        this.config = config;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            try{
                GlobalDataManager.setMappingList(config.getMappings());
                GlobalDataManager.setUdpMappingList(config.getUdpMappings());

                if (mainTcpClient == null && !config.isUseUdt()) {
                    mainTcpClient = new MainTcpClient();

                    mainTcpClient.connect(config.getHost(), config.getPort());
                }

                if (mainUdtClient == null && config.isUseUdt()) {
                    mainUdtClient = new MainUdtClient();

                    mainUdtClient.connect(config.getHost(), config.getPort());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            log.debug("Main Tcp Started...");
        }
    }
}
