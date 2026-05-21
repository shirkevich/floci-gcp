package io.floci.gcp.services.gcs.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GcsObjectMeta {

    private String kind = "storage#object";
    private String id;
    private String selfLink;
    private String name;
    private String bucket;
    private String generation;
    private String metageneration = "1";
    private String contentType;
    private String storageClass;
    private String size;
    private String timeCreated;
    private String updated;
    private String crc32c;
    private String md5Hash;
    private String mediaLink;
    private String etag;

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSelfLink() { return selfLink; }
    public void setSelfLink(String selfLink) { this.selfLink = selfLink; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getGeneration() { return generation; }
    public void setGeneration(String generation) { this.generation = generation; }

    public String getMetageneration() { return metageneration; }
    public void setMetageneration(String metageneration) { this.metageneration = metageneration; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getStorageClass() { return storageClass; }
    public void setStorageClass(String storageClass) { this.storageClass = storageClass; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getTimeCreated() { return timeCreated; }
    public void setTimeCreated(String timeCreated) { this.timeCreated = timeCreated; }

    public String getUpdated() { return updated; }
    public void setUpdated(String updated) { this.updated = updated; }

    public String getCrc32c() { return crc32c; }
    public void setCrc32c(String crc32c) { this.crc32c = crc32c; }

    public String getMd5Hash() { return md5Hash; }
    public void setMd5Hash(String md5Hash) { this.md5Hash = md5Hash; }

    public String getMediaLink() { return mediaLink; }
    public void setMediaLink(String mediaLink) { this.mediaLink = mediaLink; }

    public String getEtag() { return etag; }
    public void setEtag(String etag) { this.etag = etag; }
}
