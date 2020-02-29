package learn.architecture.remoting.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import learn.architecture.remoting.RemotingClient;
import learn.architecture.remoting.protocol.RemotingCommand;

public class NettyRemotingClient extends AbstractNettyRemoting implements RemotingClient {

    private final Bootstrap bootstrap;
    private final EventLoopGroup bossEventLoop;
    private final EventLoopGroup workerEventLoop;

    @Override
    public void sendMsgSync(String addr, RemotingCommand command, long timeoutMs) {

    }

    @Override
    public void start() {
        this.bootstrap = new Bootstrap().group(new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_RCVBUF, 65536)
                .option(ChannelOption.SO_SNDBUF, 65536)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new NettyDecoder())
                                .addLast(new NettyEncoder())
                                .addLast(new IdleStateHandler(0 ,0 , 120))
                                .addLast(new NettyConnectionHandler())
                                .addLast(new NettyClientHandler());
                    }
                });

    }

    class NettyConnectionHandler extends ChannelDuplexHandler {

    }

    class NettyClientHandler extends SimpleChannelInboundHandler<RemotingCommand> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {

        }
    }

    @Override
    public void shutdown() {

    }
}
