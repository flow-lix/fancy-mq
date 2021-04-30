package org.fancy.mq.client.common.exception;

public class SerializationException extends KafkaException {

    static final long serialVersionUID = 1L;

    public SerializationException() {
        super();
    }

    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(Throwable cause) {
        super(cause);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        // 禁用填充栈轨迹，减小抛异常的开销
        return this;
    }
}
