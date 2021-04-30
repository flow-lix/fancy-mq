package org.fancy.mq.client.common.exception;

/**
 * Kafka异常基类
 */
public class KafkaException extends RuntimeException {

    static final long serialVersionUID = 1L;

    public KafkaException() {
        super();
    }

    public KafkaException(String message) {
        super(message);
    }

    public KafkaException(Throwable cause) {
        super(cause);
    }

    public KafkaException(String message, Throwable cause) {
        super(message, cause);
    }

}
