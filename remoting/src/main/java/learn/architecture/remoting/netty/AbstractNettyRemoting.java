package learn.architecture.remoting.netty;


import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslContext;
import learn.architecture.remoting.common.NettyEvent;
import learn.architecture.remoting.common.ServiceThread;
import learn.architecture.remoting.protocol.RemotingCommand;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class AbstractNettyRemoting {

    protected volatile SslContext sslContext;

    protected EventDetectionExecutor eventDetectionExecutor;

    protected void putNettyEvent(NettyEvent event) {
        eventDetectionExecutor.putEvent(event);
    }

    protected void processMessageReceived(ChannelHandlerContext ctx, RemotingCommand msg) {

    }

    class EventDetectionExecutor extends ServiceThread {
        private BlockingQueue<NettyEvent> eventBlockingQueue = new LinkedBlockingQueue<>();

        private static final int MAX_SIZE = 2000;

        void putEvent(NettyEvent event) {
            if (eventBlockingQueue.size() <= MAX_SIZE) {
                this.eventBlockingQueue.add(event);
            } else {
                log.warn("Event queue is full, so drop current event {}", event);
            }
        }

        @Override
        public void run() {

        }

        @Override
        protected String getServerName() {
            return EventDetectionExecutor.class.getSimpleName();
        }
    }
}
