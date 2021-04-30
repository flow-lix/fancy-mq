package org.fancy.mq.client.common.serialization;

import org.fancy.mq.client.common.exception.SerializationException;
import org.fancy.mq.client.common.header.Headers;

/**
 * 对象转化为字节
 * @param <T>
 */
public interface Serializer<T> extends Cloneable {


    byte[] serialize(String topic, T data) throws SerializationException;

    /**
     * data转换为字节数组
     * @param topic data关联的topic
     * @param headers
     * @param data
     * @return
     */
    default byte[] serialize(String topic, Headers headers, T data) throws SerializationException {
        return serialize(topic, data);
    }

    /**
     * 关闭序列化器
     * 幂等方法，因为它可以被调用多次
     */
    default void close() {

    }
}
