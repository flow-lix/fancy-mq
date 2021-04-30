package org.fancy.mq.client.producer.internal;

import org.fancy.mq.client.common.TopicPartition;
import org.fancy.mq.client.common.exception.KafkaException;
import org.fancy.mq.client.common.header.Header;
import org.fancy.mq.client.common.record.Record;
import org.fancy.mq.client.producer.Callback;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RecordAccumulator {

    private volatile boolean closed;

    public RecordAppendResult append(TopicPartition tp,
                                     long timestamp,
                                     byte[] key,
                                     byte[] value,
                                     Header[] headers,
                                     Callback callback,
                                     long maxTimeToBlock,
                                     boolean abortOnNewBatch,
                                     long nowMs) throws InterruptedException {
        ByteBuffer buffer = null;
        if (headers == null) {
            headers = Record.EMPTY_HEADERS;
        }
        try {
            // check if we have an in-progress batch
            Deque<ProducerBatch> dq = getOrCreateDeque(tp);
            synchronized (dq) {
                if (closed) {
                    throw new KafkaException("Producer closed while send in progress");
                }
                RecordAppendResult appendResult = tryAppend(timestamp, key, value, headers, callback, dq, nowMs);
                if (appendResult != null) {
                    return appendResult;
                }
            }
            // we don't have an in-progress record batch try to allocate a new batch
            if (abortOnNewBatch) {
                // Return a result that will cause another call to append.
                return new RecordAppendResult(null, false);
            }
        } finally {
            if (buffer != null) {
                // toDo
//                free.deallocate(buffer);
            }
        }
    }

    private RecordAppendResult tryAppend(long timestamp, byte[] key, byte[] value, Header[] headers,
                                         Callback callback, Deque<ProducerBatch> deque, long nowMs) {
        ProducerBatch last = deque.peekLast();
        if (last != null) {
            FutureRecordMetadata future = last.tryAppend(timestamp, key, value, headers, callback, nowMs);
            if (future == null) {
                last.closeForRecordAppends();
            } else {
                return new RecordAppendResult(future, deque.size() > 1 || last.isFull());
            }
        }
        return null;
    }

    /**
     * 消息追加结果
     */
    public static class RecordAppendResult {
        public final FutureRecordMetadata future;
        public final boolean batchIsFull;
        public final boolean newBatchCreated;

        public RecordAppendResult(FutureRecordMetadata future, boolean batchIsFull, boolean newBatchCreated) {
            this.future = future;
            this.batchIsFull = batchIsFull;
            this.newBatchCreated = newBatchCreated;
        }
    }

    private ConcurrentMap<TopicPartition, Deque<ProducerBatch>> batchs = new ConcurrentHashMap<>();

    private Deque<ProducerBatch> getOrCreateDeque(TopicPartition tp) {
        return batchs.computeIfAbsent(tp, p  -> new ArrayDeque<>());
    }
}
