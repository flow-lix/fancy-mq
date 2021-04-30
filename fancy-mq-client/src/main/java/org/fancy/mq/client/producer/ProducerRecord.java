package org.fancy.mq.client.producer;

import org.fancy.mq.client.common.header.Headers;

/**
 * 生产者发送的消息
 * @param <K> msg key
 * @param <V> msg val
 */
public class ProducerRecord<K, V> {

    private final String topic;
    private final Integer partition;
    private final K key;
    private final V value;
    private final Headers headers;

    // 记录的发送时间戳，如果未设置就用系统当前时间
    private final Long timestamp;

    public ProducerRecord(String topic, K key, V value) {
        this(topic, null, key, value);
    }

    public ProducerRecord(String topic, Integer partition, K key, V value) {
        this(topic, partition, key, value, null, null);
    }

    public ProducerRecord(String topic, Integer partition, K key, V value, Headers headers, Long timestamp) {
        this.topic = topic;
        this.partition = partition;
        this.key = key;
        this.value = value;
        this.headers = headers;
        this.timestamp = timestamp;
    }

    public String topic() {
        return topic;
    }

    public Integer partition() {
        return partition;
    }

    public K key() {
        return key;
    }


    public V value() {
        return value;
    }

    public Headers headers() {
        return headers;
    }

    public Long timestamp() {
        return timestamp;
    }
}
