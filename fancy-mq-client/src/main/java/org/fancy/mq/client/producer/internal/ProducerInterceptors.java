package org.fancy.mq.client.producer.internal;

import org.fancy.mq.client.producer.ProducerInterceptor;
import org.fancy.mq.client.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 持有生产者拦截器的容器，封装了自定义拦截器的调用
 */
public class ProducerInterceptors<K, V> {

    private static final Logger log = LoggerFactory.getLogger(ProducerInterceptors.class);

    private final List<ProducerInterceptor<K, V>> interceptors;

    public ProducerInterceptors(List<ProducerInterceptor<K, V>> interceptors) {
        this.interceptors = interceptors;
    }

    /**
     * 调用链 职责链设计模式
     * @param record
     * @return
     */
    public ProducerRecord<K, V> onSend(ProducerRecord<K, V> record) {
        ProducerRecord<K, V> interceptRecord = record;
        for (ProducerInterceptor<K, V> interceptor : this.interceptors) {
            try {
                interceptRecord = interceptor.onSend(interceptRecord);
            } catch (Exception e) {
                if (record != null) {
                    log.warn("执行拦截器OnSend回调方法时出错，topic: {}, partition: {}", record.topic(), record.partition(), e);
                } else {
                    log.warn("执行拦截器OnSend回调方法时出错", e);
                }
            }
        }
        return interceptRecord;
    }
}
