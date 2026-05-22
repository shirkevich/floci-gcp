package io.floci.gcp.services.kafka.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.LinkedHashMap;
import java.util.Map;

@RegisterForReflection
public class StoredConsumerGroup {

    private String name;
    private Map<String, ConsumerTopicMetadata> topics = new LinkedHashMap<>();

    public StoredConsumerGroup() {}

    public StoredConsumerGroup(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Map<String, ConsumerTopicMetadata> getTopics() { return topics; }
    public void setTopics(Map<String, ConsumerTopicMetadata> topics) { this.topics = topics; }

    @RegisterForReflection
    public static class ConsumerTopicMetadata {
        private Map<String, ConsumerPartitionMetadata> partitions = new LinkedHashMap<>();

        public Map<String, ConsumerPartitionMetadata> getPartitions() { return partitions; }
        public void setPartitions(Map<String, ConsumerPartitionMetadata> partitions) { this.partitions = partitions; }
    }

    @RegisterForReflection
    public static class ConsumerPartitionMetadata {
        private long offset;

        public ConsumerPartitionMetadata() {}
        public ConsumerPartitionMetadata(long offset) { this.offset = offset; }

        public long getOffset() { return offset; }
        public void setOffset(long offset) { this.offset = offset; }
    }
}
