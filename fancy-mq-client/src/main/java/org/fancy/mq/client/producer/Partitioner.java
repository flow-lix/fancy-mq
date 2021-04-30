package org.fancy.mq.client.producer;

import org.fancy.mq.client.common.Cluster;

public interface Partitioner {

    int partition(String topic, Cluster cluster);

}
