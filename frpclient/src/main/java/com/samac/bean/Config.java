package com.samac.bean;

/*
 * Copyright (C) 2024 samac199999
 * All rights reserved.
 *
 * Contact: samac199999@gmail.com
 *
 * This code is licensed under the MIT License.
 * See the LICENSE file in the project root for more information.
 */

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "config")
public class Config {
    private String host;
    private int port;
    private String token;
    private boolean useUdt;
    private String password;
    private List<String> mappings;
    private List<String> udpMappings;
	
	private long writeLimit = 0;
    private long readLimit = 0;

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setUseUdt(boolean useUdt) {
        this.useUdt = useUdt;
    }

    public boolean isUseUdt() {
        return useUdt;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setMappings(List<String> mappings) {
        this.mappings = mappings;
    }

    public List<String> getMappings() {
        return mappings;
    }

    public long getWriteLimit() {
        return writeLimit;
    }

    public void setWriteLimit(long writeLimit) {
        this.writeLimit = writeLimit;
    }

    public long getReadLimit() {
        return readLimit;
    }

    public void setReadLimit(long readLimit) {
        this.readLimit = readLimit;
    }

    public void setUdpMappings(List<String> udpMappings) {
        this.udpMappings = udpMappings;
    }

    public List<String> getUdpMappings() {
        return udpMappings;
    }
}
