package org.fancy.mq.client.producer.internal;

import org.fancy.mq.client.common.TopicPartition;
import org.fancy.mq.client.common.header.Header;
import org.fancy.mq.client.common.record.MemoryRecordBuilder;
import org.fancy.mq.client.producer.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 1. 将要发送的一批消息
 * 2. 非线程安全类，修改它时必须要进行同步
 */
public class ProducerBatch {

    public static final Logger log = LoggerFactory.getLogger(ProducerBatch.class);




    private enum FinalState { ABORTED, FAILED, SUCCESSED }

    /**
     * 这批消息属于哪个分区
     */
    private final TopicPartition topicPartition;

    private final MemoryRecordBuilder recordBuilder;

    /**
     * 这批存了多少条消息
     */
    private int recordCount;

    private AtomicReference<FinalState> finalState = new AtomicReference<>(null);

    /**
     * 尝试追加一条消息
     * @param timestamp
     * @param key
     * @param value
     * @param headers
     */
    public FutureRecordMetadata tryAppend(long timestamp, byte[] key, byte[] value, Header[] headers, Callback callback, long now) {

    }

    public boolean isFull() {
        return recordBuilder.isFull();
    }

    public void closeForRecordAppends() {
        recordBuilder.closeForRecordAppends();
    }
}
