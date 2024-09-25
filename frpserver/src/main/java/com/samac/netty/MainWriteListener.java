package com.samac.netty;

import com.samac.common.ProtocolMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public class MainWriteListener implements ChannelFutureListener {

    private final Channel mainChannel;
    private final ProtocolMessage msg;

    public MainWriteListener(Channel mainChannel, ProtocolMessage msg) {
        this.mainChannel = mainChannel;
        this.msg = msg;
    }

    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if (channelFuture.isSuccess()) {
            ProtocolMessage newMsg = new ProtocolMessage();
            newMsg.setProtocol(ProtocolMessage.P_TRANS_DATA_ACK);
            newMsg.setReserved(msg.getReserved());
            newMsg.setReserved1(msg.getReserved1());
            if (mainChannel != null) {
                mainChannel.writeAndFlush(msg);
            }
        }
    }
}
