package org.fancy.mq.client.common;

/**
 * 主题分区
 */
public class TopicPartition {

    private final String topic;
    private final Integer partition;

    public TopicPartition(String topic, Integer partition) {
        this.topic = topic;
        this.partition = partition;
    }
}
