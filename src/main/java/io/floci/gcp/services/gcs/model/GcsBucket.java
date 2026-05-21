package io.floci.gcp.services.gcs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GcsBucket {

    private String kind = "storage#bucket";
    private String id;
    private String selfLink;
    private String name;
    private String projectNumber;
    private String metageneration = "1";
    private String location;
    private String storageClass;
    private String timeCreated;
    private String updated;
    private String etag;
    private String projectId;

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSelfLink() { return selfLink; }
    public void setSelfLink(String selfLink) { this.selfLink = selfLink; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProjectNumber() { return projectNumber; }
    public void setProjectNumber(String projectNumber) { this.projectNumber = projectNumber; }

    public String getMetageneration() { return metageneration; }
    public void setMetageneration(String metageneration) { this.metageneration = metageneration; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStorageClass() { return storageClass; }
    public void setStorageClass(String storageClass) { this.storageClass = storageClass; }

    public String getTimeCreated() { return timeCreated; }
    public void setTimeCreated(String timeCreated) { this.timeCreated = timeCreated; }

    public String getUpdated() { return updated; }
    public void setUpdated(String updated) { this.updated = updated; }

    public String getEtag() { return etag; }
    public void setEtag(String etag) { this.etag = etag; }

    @JsonIgnore
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
}
