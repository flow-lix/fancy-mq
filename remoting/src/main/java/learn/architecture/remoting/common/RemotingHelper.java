package learn.architecture.remoting.common;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

@Slf4j
public class RemotingHelper {

    public static final String OS_NAME = System.getProperty("os.name");

    private static boolean linuxPlatform = false;
    private static boolean windowPlatform = false;

    static {
        if (OS_NAME != null) {
            if (OS_NAME.toLowerCase().contains("linux")) {
                linuxPlatform = true;
            } else if (OS_NAME.toLowerCase().contains("window")) {
                windowPlatform = true;
            }
        }
    }
    public static String parseRemotingAddress(Channel channel) {
        SocketAddress sa = channel.remoteAddress();
        if (sa == null) {
            return "";
        }
        return sa.toString();
    }

    public static void closeChannel(Channel channel) {
        final String remotingAddr = parseRemotingAddress(channel);
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("Close the connection, remoting address: {}, result: {}", remotingAddr, future.isSuccess());
            }
        });
    }

    public static boolean isLinuxPlatform() {
        return linuxPlatform;
    }
}
