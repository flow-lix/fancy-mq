package org.fancy.mq.client.producer;

public interface ProducerInterceptor<K, V> {

    ProducerRecord<K,V> onSend(ProducerRecord<K, V> record);
}
