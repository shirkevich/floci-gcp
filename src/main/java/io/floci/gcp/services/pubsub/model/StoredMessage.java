package io.floci.gcp.services.pubsub.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Map;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoredMessage {

    private String messageId;
    private byte[] data;
    private Map<String, String> attributes;
    private String publishTime;
    private String orderingKey;

    public StoredMessage() {}

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }

    public String getPublishTime() { return publishTime; }
    public void setPublishTime(String publishTime) { this.publishTime = publishTime; }

    public String getOrderingKey() { return orderingKey; }
    public void setOrderingKey(String orderingKey) { this.orderingKey = orderingKey; }
}
