package com.samac.common;

/*
 * Copyright (C) 2024 samac199999
 * All rights reserved.
 *
 * Contact: samac199999@gmail.com
 *
 * This code is licensed under the MIT License.
 * See the LICENSE file in the project root for more information.
 */

import com.samac.netty.*;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Constants {
    public static final AttributeKey<Map<Short, PortUseBase>> PORT_MAPPING_ATTRIBUTE_KEY = AttributeKey.valueOf("port-mapping");
    public static final AttributeKey<Channel> MAIN_CHANNEL_ATTRIBUTE_KEY = AttributeKey.valueOf("main-ch");
    public static final AttributeKey<Map<Short, Map<Integer, Channel>>> CHILD_CHANNEL_ATTRIBUTE_KEY = AttributeKey.valueOf("child-ch");
    public static final AttributeKey<PortUseBase> CHILD_PORT_MAPPING_ATTRIBUTE_KEY = AttributeKey.valueOf("child-port-mapping");
    public static final AttributeKey<Integer> CHILD_INDEX_ATTRIBUTE_KEY = AttributeKey.valueOf("child-index");
    public static final AttributeKey<AtomicInteger> ATOMIC_INTEGER_ATTRIBUTE_KEY = AttributeKey.valueOf("atomic-int");
    public static final AttributeKey<TcpServer> TCP_SERVER_ATTRIBUTE_KEY = AttributeKey.valueOf("tcp-server");
    public static final AttributeKey<ChannelWriteListener> CHANNEL_WRITE_LISTENER_ATTRIBUTE_KEY = AttributeKey.valueOf("ch-w-listener");
    public static final AttributeKey<UdpServer> UDP_SERVER_ATTRIBUTE_KEY = AttributeKey.valueOf("udp-server");
}
