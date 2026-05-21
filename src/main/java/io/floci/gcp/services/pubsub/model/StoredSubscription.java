package io.floci.gcp.services.pubsub.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoredSubscription {

    private String name;
    private String topic;
    private int ackDeadlineSeconds = 10;

    public StoredSubscription() {}

    public StoredSubscription(String name, String topic, int ackDeadlineSeconds) {
        this.name = name;
        this.topic = topic;
        this.ackDeadlineSeconds = ackDeadlineSeconds;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public int getAckDeadlineSeconds() { return ackDeadlineSeconds; }
    public void setAckDeadlineSeconds(int ackDeadlineSeconds) { this.ackDeadlineSeconds = ackDeadlineSeconds; }
}
