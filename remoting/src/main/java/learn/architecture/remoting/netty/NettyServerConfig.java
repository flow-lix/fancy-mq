package learn.architecture.remoting.netty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NettyServerConfig {

    public static final String HANDSHAKE_NAME = "handshakeHandler";
    public static final String TLS_HANDLER_NAME = "sslHandler";

    public static final String FILE_REGION_ENCODER_NAME = "fileRegionEncoder";

    private int listenPort = 8888;

    private int serverChannelMaxIdleTimeSeconds = 120;

    private boolean epollAvailable = false;
    private boolean serverPooledByteBufAllocatorEnable = true;

    private int serverWorkerThread = 8;

}
