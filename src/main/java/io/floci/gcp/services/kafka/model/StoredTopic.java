package io.floci.gcp.services.kafka.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Map;

@RegisterForReflection
public class StoredTopic {

    private String name;
    private int partitionCount;
    private int replicationFactor;
    private Map<String, String> configs;

    public StoredTopic() {}

    public StoredTopic(String name, int partitionCount, int replicationFactor) {
        this.name = name;
        this.partitionCount = partitionCount;
        this.replicationFactor = replicationFactor;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPartitionCount() { return partitionCount; }
    public void setPartitionCount(int partitionCount) { this.partitionCount = partitionCount; }

    public int getReplicationFactor() { return replicationFactor; }
    public void setReplicationFactor(int replicationFactor) { this.replicationFactor = replicationFactor; }

    public Map<String, String> getConfigs() { return configs; }
    public void setConfigs(Map<String, String> configs) { this.configs = configs; }
}
