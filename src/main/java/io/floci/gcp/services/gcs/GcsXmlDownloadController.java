package io.floci.gcp.services.gcs;

import io.floci.gcp.services.gcs.model.GcsObjectMeta;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Handles GCS XML API downloads: GET /{bucket}/{object}.
 * The Google Cloud Go SDK uses this URL format when STORAGE_EMULATOR_HOST is set.
 */
@ApplicationScoped
@Path("/{bucket: [a-z0-9._-]+}")
public class GcsXmlDownloadController {

    private final GcsService service;

    @Inject
    public GcsXmlDownloadController(GcsService service) {
        this.service = service;
    }

    @GET
    @Path("/{object: .+}")
    public Response download(
            @PathParam("bucket") String bucket,
            @PathParam("object") String objectPath) {
        String objectName = URLDecoder.decode(objectPath, StandardCharsets.UTF_8);
        byte[] data = service.getObjectData(bucket, objectName);
        GcsObjectMeta meta = service.getObjectMeta(bucket, objectName);
        return Response.ok(data).type(meta.getContentType()).build();
    }
}
