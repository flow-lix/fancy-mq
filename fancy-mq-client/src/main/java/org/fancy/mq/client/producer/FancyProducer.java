package org.fancy.mq.client.producer;

import org.fancy.mq.client.common.Cluster;
import org.fancy.mq.client.common.TopicPartition;
import org.fancy.mq.client.common.exception.KafkaException;
import org.fancy.mq.client.common.header.Header;
import org.fancy.mq.client.common.serialization.Serializer;
import org.fancy.mq.client.producer.internal.FutureRecordMetadata;
import org.fancy.mq.client.producer.internal.ProducerInterceptors;
import org.fancy.mq.client.producer.internal.RecordAccumulator;
import org.fancy.mq.client.producer.internal.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public final class FancyProducer<K, V> implements Producer<K, V> {

    private static final Logger log = LoggerFactory.getLogger(FancyProducer.class);

    private final ProducerInterceptors<K, V> interceptors;
    private final Sender sender;
    private final Serializer<K> keySerializer;
    private final Serializer<V> valueSerializer;

    private final Partitioner partitioner;
    // 消息累加器
    private final RecordAccumulator accumulator;

    public FancyProducer(ProducerInterceptors<K, V> interceptors) {
        this.interceptors = interceptors;
    }

    /**
     * Asynchronously send a record to a topic. Equivalent to <code>send(record, null)</code>.
     * See {@link #send(ProducerRecord, Callback)} for details.
     */
    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
        return send(record, null);
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback) {
        // intercept the record, which can be potentially modified; this method does not throw exceptions
        ProducerRecord<K, V> interceptedRecord = this.interceptors.onSend(record);
        return doSend(interceptedRecord, callback);
    }

    /**
     * Implementation of asynchronously send a record to a topic.
     * 1.序列化
     * 2.选择分区
     * 3.追加到消息累加器
     * 4.通知Sender线程批量发送消息
     */
    private Future<RecordMetadata> doSend(ProducerRecord<K, V> record, Callback callback) {
        TopicPartition tp = null;
        try {
            throwIfProducerClosed();
            // first make sure the metadata for the topic is available
            long nowMs = time.milliseconds();
            ClusterAndWaitTime clusterAndWaitTime;
            try {
                clusterAndWaitTime = waitOnMetadata(record.topic(), record.partition(), nowMs, maxBlockTimeMs);
            } catch (KafkaException e) {
                if (metadata.isClosed())
                    throw new KafkaException("FancyProducer closed while send in progress", e);
                throw e;
            }
            nowMs += clusterAndWaitTime.waitedOnMetadataMs;
            long remainingWaitMs = Math.max(0, maxBlockTimeMs - clusterAndWaitTime.waitedOnMetadataMs);
            Cluster cluster = clusterAndWaitTime.cluster;


            byte[] serializedKey = keySerializer.serialize(record.topic(), record.headers(), record.key());
            byte[] serializedValue = valueSerializer.serialize(record.topic(), record.headers(), record.value());

            int partition = partition(record, serializedKey, serializedValue, cluster);
            tp = new TopicPartition(record.topic(), partition);

//            setReadOnly(record.headers());
            Header[] headers = record.headers().toArray();
//            int serializedSize = AbstractRecords.estimateSizeInBytesUpperBound(apiVersions.maxUsableProduceMagic(),
//                    compressionType, serializedKey, serializedValue, headers);
//            ensureValidRecordSize(serializedSize);
            long timestamp = record.timestamp() == null ? nowMs : record.timestamp();
//            if (log.isTraceEnabled()) {
//                log.trace("Attempting to append record {} with callback {} to topic {} partition {}", record, callback, record.topic(), partition);
//            }
//            // producer callback will make sure to call both 'callback' and interceptor callback
            Callback interceptCallback = new InterceptorCallback<>(callback, this.interceptors, tp);
//            if (transactionManager != null && transactionManager.isTransactional()) {
//                transactionManager.failIfNotReadyForSend();
//            }

            RecordAccumulator.RecordAppendResult result = accumulator.append(tp, timestamp, serializedKey,
                    serializedValue, headers, interceptCallback, remainingWaitMs, true, nowMs);

//            if (result.abortForNewBatch) {
//                int prevPartition = partition;
//                partitioner.onNewBatch(record.topic(), cluster, prevPartition);
//                partition = partition(record, serializedKey, serializedValue, cluster);
//                tp = new TopicPartition(record.topic(), partition);
//                if (log.isTraceEnabled()) {
//                    log.trace("Retrying append due to new batch creation for topic {} partition {}. The old partition was {}", record.topic(), partition, prevPartition);
//                }
//                // producer callback will make sure to call both 'callback' and interceptor callback
//                interceptCallback = new InterceptorCallback<>(callback, this.interceptors, tp);
//
//                result = accumulator.append(tp, timestamp, serializedKey,
//                        serializedValue, headers, interceptCallback, remainingWaitMs, false, nowMs);
//            }

            if (result.batchIsFull || result.newBatchCreated) {
                log.trace("Waking up the sender since topic {} partition {} is either full or getting a new batch", record.topic(), partition);
                this.sender.wakeup();
            }
            return result.future;
        } catch (ApiException e) {
            log.debug("Exception occurred during message send:", e);
            if (callback != null)
                callback.onCompletion(null, e);
            this.errors.record();
            this.interceptors.onSendError(record, tp, e);
            return new FutureFailure(e);
        } catch (InterruptedException e) {
            this.errors.record();
            this.interceptors.onSendError(record, tp, e);
            throw new InterruptException(e);
        } catch (KafkaException e) {
            this.errors.record();
            this.interceptors.onSendError(record, tp, e);
            throw e;
        } catch (Exception e) {
            // we notify interceptor about all exceptions, since onSend is called before anything else in this method
            this.interceptors.onSendError(record, tp, e);
            throw e;
        }
    }

    private int partition(ProducerRecord<K, V> record, byte[] serializedKey, byte[] serializedValue, Cluster cluster) {
        return record.partition() != null ?
                record.partition() :
                partitioner.partition(record.topic(), cluster);
    }

    private void throwIfProducerClosed() {
        if (sender == null || !sender.isRunning()) {
            throw new IllegalStateException("生产者未启动，不能执行下一步操作");
        }
    }

    /**
     * 生产者发送成功回调和拦截器回调
     */
    private class InterceptorCallback<K, V> implements Callback {
        private final Callback userCallback;
        private final ProducerInterceptors<K, V> interceptors;
        private final TopicPartition tp;

        public InterceptorCallback(Callback userCallback, ProducerInterceptors<K, V> interceptors, TopicPartition tp) {
            this.userCallback = userCallback;
            this.interceptors = interceptors;
            this.tp = tp;
        }

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            log.info("user callback completion");
            if (userCallback != null) {
                userCallback.onCompletion(metadata, exception);
            }
        }
    }
}
