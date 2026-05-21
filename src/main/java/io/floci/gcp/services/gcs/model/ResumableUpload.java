package io.floci.gcp.services.gcs.model;

public record ResumableUpload(String bucket, String objectName, String contentType) {}
