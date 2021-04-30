package org.fancy.mq.client.common;

/**
 * 元数据服务返回的分区信息
 */
public class PartitionInfo {

    /**
     * 主题
     */
    private String topic;
    /**
     * 分区编号
     */
    private int partition;
    /**
     * lead分区的节点信息
     */
    private Node leader;
    /**
     * 所有分区的节点信息
     */
    private Node[] replicas;
    /**
     * 已同步的分区节点
     */
    private Node[] inSyncReplicas;
    /**
     * 未同步的分区节点
     */
    private Node[] offlineReplicas;

    public String getTopic() {
        return topic;
    }

    public int partition() {
        return partition;
    }

    public Node getLeader() {
        return leader;
    }

    public Node[] getReplicas() {
        return replicas;
    }

    public Node[] getInSyncReplicas() {
        return inSyncReplicas;
    }

    public Node[] getOfflineReplicas() {
        return offlineReplicas;
    }
}
