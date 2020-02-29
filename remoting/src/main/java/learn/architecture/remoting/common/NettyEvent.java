package learn.architecture.remoting.common;

import io.netty.channel.Channel;
import learn.architecture.remoting.netty.NettyEventType;

public class NettyEvent {

    private NettyEventType eventType;
    private String remotingAddr;
    private Channel channel;

    public NettyEvent(NettyEventType eventType, String remotingAddr, Channel channel) {
        this.eventType = eventType;
        this.remotingAddr = remotingAddr;
        this.channel = channel;
    }
}
