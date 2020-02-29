package learn.architecture.remoting.common;

public abstract class ServiceThread implements Runnable {

    private Thread thread;

    public ServiceThread() {
        this.thread = new Thread(this, this.getServerName());
    }

    protected abstract String getServerName();

    protected void start() {
        this.thread.start();
    }


}
