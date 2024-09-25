package com.samac.netty;

import com.samac.common.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class TcpClientChannelFutureListener implements ChannelFutureListener {

    private final Channel mainChannel;
    private final PortUseBase portMapping;
    private final int childIndex;

    public TcpClientChannelFutureListener(Channel mainChannel, PortUseBase portMapping, int childIndex) {
        this.mainChannel = mainChannel;
        this.portMapping = portMapping;
        this.childIndex = childIndex;
    }

    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if (channelFuture.isSuccess()) {
            //log.info("operationComplete isSuccess {}", channelFuture.channel());
            Channel myChannel = channelFuture.channel();
            myChannel.attr(Constants.MAIN_CHANNEL_ATTRIBUTE_KEY).set(mainChannel);
            myChannel.attr(Constants.CHILD_PORT_MAPPING_ATTRIBUTE_KEY).set(portMapping);
            myChannel.attr(Constants.CHILD_INDEX_ATTRIBUTE_KEY).set(childIndex);
            Map<Integer, Channel> childChMap = mainChannel.attr(Constants.CHILD_CHANNEL_ATTRIBUTE_KEY).get().get(portMapping.getIndex());
            childChMap.put(childIndex, myChannel);
            if (portMapping instanceof PortMapping) {
                ProtocolMessage msg = new ProtocolMessage();
                msg.setProtocol(ProtocolMessage.P_CONNECT_SUCCESS);
                msg.setReserved(portMapping.getIndex());
                msg.setReserved1(childIndex);
                mainChannel.writeAndFlush(msg);
            }
        } else {
            if (mainChannel.isOpen() && mainChannel.isActive()) {
                ProtocolMessage msg = new ProtocolMessage();
                msg.setProtocol(ProtocolMessage.P_CONNECT_FAIL);
                msg.setReserved(portMapping.getIndex());
                msg.setReserved1(childIndex);
                mainChannel.writeAndFlush(msg);
            }
        }
    }
}
