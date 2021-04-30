package org.fancy.mq.client.producer;

import org.fancy.mq.client.common.Cluster;
import org.fancy.mq.client.common.PartitionInfo;
import org.fancy.mq.client.common.utils.Utils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询调度算法
 */
public class RoundRobinPartitioner implements Partitioner {

    private ConcurrentMap<String, AtomicInteger> topicCounterMap = new ConcurrentHashMap<>();

    @Override
    public int partition(String topic, Cluster cluster) {
        // 防止int溢出
        int nextValue = Utils.toPositive(nextValue(topic));
        List<PartitionInfo> availablePartitions = cluster.availablePartitions(topic);
        if (!availablePartitions.isEmpty()) {
            int part = nextValue % availablePartitions.size();
            return availablePartitions.get(part).partition();
        }
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();
        int part = nextValue % numPartitions;
        return partitions.get(part).partition();
    }

    private int nextValue(String topic) {
        AtomicInteger counter = topicCounterMap.computeIfAbsent(topic, k ->
                new AtomicInteger(0));
        return counter.incrementAndGet();
    }

}
