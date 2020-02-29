package learn.architecture.remoting.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import learn.architecture.remoting.ChannelEventListener;
import learn.architecture.remoting.common.NettyEvent;
import learn.architecture.remoting.common.RemotingHelper;
import learn.architecture.remoting.common.TlsMode;
import learn.architecture.remoting.protocol.RemotingCommand;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;

import static learn.architecture.remoting.common.TlsMode.ENFORCE;

@Slf4j
public class NettyRemotingServer extends AbstractNettyRemoting {

    private int port;

    private final ServerBootstrap bootstrap;
    private final EventLoopGroup bossEventLoop;
    private final EventLoopGroup workerEventLoop;

    private final NettyServerConfig nettyServerConfig;

    private DefaultEventExecutorGroup eventExecutors;

    private ChannelEventListener channelEventListener;

    /**
     * 共享的Handlers
     */
    private HandSharkHandler handSharkHandler;
    private ConnectionHandler connectionHandler;
    private NettyServerHandler serverHandler;

    public NettyRemotingServer(NettyServerConfig nettyServerConfig, ChannelEventListener eventListener) {
        this.bootstrap = new ServerBootstrap();
        if (useEpoll()) {
            this.bossEventLoop = new EpollEventLoopGroup(1,
                    new DefaultThreadFactory("fancymq-server-epoll-boss", false));
            this.workerEventLoop = new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2,
                    new DefaultThreadFactory("fancymq-server-epoll-worker", false));
        } else {
            this.bossEventLoop = new NioEventLoopGroup(1,
                    new DefaultThreadFactory("fancyrpc-server-nio-boss", false);
            this.workerEventLoop = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2,
                    new DefaultThreadFactory("fancymq-server-nio-worker", false));
        }
        this.nettyServerConfig = nettyServerConfig;

        loadTlsContext();
    }

    private void loadTlsContext() {
        TlsMode tlsMode = TlsSystemConfig.tlsMode;
        log.info("Server is running in TLS {} mode.", tlsMode);
        if (tlsMode == TlsMode.DISABLE) {
            return;
        }
        try {
            TlsHelper.buildSslContext();
            log.info("Ssl context created for server.");
        } catch (IOException e) {
            log.info("Failed to create ssl context!");
        }
    }

    public void start() {
        this.eventExecutors = new DefaultEventExecutorGroup(nettyServerConfig.getServerWorkerThread(),
                new DefaultThreadFactory("netty-server-codec"));

        prepareSharableHandlers();

        this.bootstrap.group(bossEventLoop, workerEventLoop)
                .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_SNDBUF, )
                .childOption(ChannelOption.SO_RCVBUF)
                .localAddress(new InetSocketAddress(nettyServerConfig.getListenPort()))
                .handler(new ChannelInitializer<ServerSocketChannel>() {
                    @Override
                    protected void initChannel(ServerSocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(eventExecutors, NettyServerConfig.HANDSHAKE_NAME, handSharkHandler)
                                .addLast(new NettyEncoder())
                                .addLast(new NettyDecoder())
                                .addLast(new IdleStateHandler(0 ,0, nettyServerConfig.getServerChannelMaxIdleTimeSeconds()))
                    }
                });
        if (nettyServerConfig.isServerPooledByteBufAllocatorEnable()) {
            this.bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }

        try {
            ChannelFuture future = this.bootstrap.bind().sync();
            this.port = ((InetSocketAddress)(future.channel().localAddress())).getPort();
        } catch (InterruptedException e) {
            throw new RuntimeException("The server bootstrap bind address sync error!", e);
        }

        if (channelEventListener != null) {
            eventDetectionExecutor.start();
        }


    }

    /**'
     * 以下3个handler都没有状态，所以是共享的
     */
    private void prepareSharableHandlers() {
        handSharkHandler = new HandSharkHandler(TlsMode.DISABLE);
        connectionHandler = new ConnectionHandler();
        serverHandler = new NettyServerHandler();
    }

    private boolean useEpoll() {
        return RemotingHelper.isLinuxPlatform() &&
                nettyServerConfig.isEpollAvailable() &&
                Epoll.isAvailable();
    }

    @ChannelHandler.Sharable
    class HandSharkHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private final TlsMode tlsMode;

        private static final byte HANDSHAKE_MAGIC_CODE = 0x16;

        HandSharkHandler(TlsMode tlsMode) {
            this.tlsMode = tlsMode;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            msg.markReaderIndex();

            byte first = msg.getByte(1);
            if (first == HANDSHAKE_MAGIC_CODE) {
                switch (tlsMode) {
                    case DISABLE:
                        log.warn("DISABLE 模式不能建立SSL连接!");
                        ctx.close();
                        break;
                    case PERMISSIVE:
                    case ENFORCE:
                        if (sslContext != null) {
                            ctx.pipeline().addAfter(eventExecutors, NettyServerConfig.HANDSHAKE_NAME,
                                    NettyServerConfig.TLS_HANDLER_NAME, sslContext.newHandler(ctx.alloc()))
                            .addAfter(eventExecutors, NettyServerConfig.TLS_HANDLER_NAME,
                                    NettyServerConfig.FILE_REGION_ENCODER_NAME, new FileRegionEncoder());
                            log.info("Channel pipeline已准备好SslHandler来建立Ssl连接");
                        } else {
                            ctx.close();
                            log.warn("尝试建立SSL连接，但sslContext是空的!");
                        }
                        break;
                    default:
                       log.warn("未知的mode: {}", first);
                       break;
                }
            } else if (tlsMode == ENFORCE){
                ctx.close();
                log.warn("客户端想要建立一个不安全的连接，而且服务运行在ENFORCE模式!");
            }
            msg.resetReaderIndex();

            ctx.pipeline().remove(this);
            ctx.fireChannelRead(msg.retain()); // msg引用自增一次
        }
    }

    @ChannelHandler.Sharable
    class ConnectionHandler extends ChannelDuplexHandler {
        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            final String remotingAddr = RemotingHelper.parseRemotingAddress(ctx.channel());
            log.debug("Channel registered, remoting address: {}", remotingAddr);
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            final String remotingAddr = RemotingHelper.parseRemotingAddress(ctx.channel());
            log.debug("Channel unregistered, remoting address: {}", remotingAddr);
            super.channelUnregistered(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            final String remotingAddr = RemotingHelper.parseRemotingAddress(ctx.channel());
            log.debug("Channel active, remoting address: {}", remotingAddr);
            super.channelActive(ctx);
            if (channelEventListener != null) {
                NettyRemotingServer.this.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remotingAddr, ctx.channel()));
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            final String remotingAddr = RemotingHelper.parseRemotingAddress(ctx.channel());
            log.debug("Channel inactive, remoting address: {}", remotingAddr);
            super.channelInactive(ctx);
            if (channelEventListener != null) {
                putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remotingAddr, ctx.channel()));
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                if (((IdleStateEvent) evt).state() == IdleState.ALL_IDLE) {
                    final String remotingAddr = RemotingHelper.parseRemotingAddress(ctx.channel());
                    log.debug("Use event triggered, remoting address: {}", remotingAddr);
                    // close
                    RemotingHelper.closeChannel(ctx.channel());
                    putNettyEvent(new NettyEvent(NettyEventType.IDLE, remotingAddr, ctx.channel()));
                }
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final String remotingAddr = RemotingHelper.parseRemotingAddress(ctx.channel());
            log.info("Exception caught: {} from remoting: {}", cause, remotingAddr);

            if (channelEventListener != null) {
                putNettyEvent(new NettyEvent(NettyEventType.EXCEPTION, remotingAddr, ctx.channel()));
            }
            RemotingHelper.closeChannel(ctx.channel());
        }
    }

    @ChannelHandler.Sharable
    class NettyServerHandler extends SimpleChannelInboundHandler<RemotingCommand> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
            processMessageReceived(ctx, msg);
        }
    }
}
