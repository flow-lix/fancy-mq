package org.fancy.mq.client.producer;

import java.util.concurrent.Future;

/**
 * 生产者接口
 */
public interface Producer<K, V> {

    Future<RecordMetadata> send(ProducerRecord<K, V> record);

    Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback);
}
