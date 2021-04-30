package org.fancy.mq.client.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Kafka集群中topic、partition、node的子集
 * 不可变的表示
 */
public class Cluster {

    private Map<String, List<PartitionInfo>> partitionsByTopic;
    private Map<String, List<PartitionInfo>> availablePartitionsByTopic;

    public List<PartitionInfo> partitionsForTopic(String topic) {
        return partitionsByTopic.getOrDefault(topic, Collections.emptyList());
    }

    public List<PartitionInfo> availablePartitions(String topic) {
        return availablePartitionsByTopic.getOrDefault(topic, Collections.emptyList());
    }

}
