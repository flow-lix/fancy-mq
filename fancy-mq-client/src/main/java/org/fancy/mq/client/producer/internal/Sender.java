package org.fancy.mq.client.producer.internal;

public class Sender implements Runnable {

    private volatile boolean running;

    @Override
    public void run() {

    }

    public boolean isRunning() {
        return running;
    }

    public void wakeup() {

    }
}
