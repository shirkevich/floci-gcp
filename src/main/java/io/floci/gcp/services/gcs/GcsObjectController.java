package io.floci.gcp.services.gcs;

import io.floci.gcp.config.EmulatorConfig;
import io.floci.gcp.core.common.GcpException;
import io.floci.gcp.services.gcs.model.GcsObjectMeta;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Path("/storage/v1/b/{bucket}/o")
@Produces(MediaType.APPLICATION_JSON)
public class GcsObjectController {

    private final GcsService service;
    private final EmulatorConfig config;

    @Inject
    public GcsObjectController(GcsService service, EmulatorConfig config) {
        this.service = service;
        this.config = config;
    }

    @GET
    public Response listObjects(@PathParam("bucket") String bucket) {
        List<GcsObjectMeta> items = service.listObjects(bucket);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kind", "storage#objects");
        if (!items.isEmpty()) {
            response.put("items", items);
        }
        return Response.ok(response).build();
    }

    @GET
    @Path("/{object: .+}")
    public Response getObject(
            @PathParam("bucket") String bucket,
            @PathParam("object") String objectPath,
            @QueryParam("alt") String alt) {
        String objectName = URLDecoder.decode(objectPath, StandardCharsets.UTF_8);
        if ("media".equals(alt)) {
            byte[] data = service.getObjectData(bucket, objectName);
            GcsObjectMeta meta = service.getObjectMeta(bucket, objectName);
            return Response.ok(data).type(meta.getContentType()).build();
        }
        return Response.ok(service.getObjectMeta(bucket, objectName)).build();
    }

    @DELETE
    @Path("/{object: .+}")
    public Response deleteObject(
            @PathParam("bucket") String bucket,
            @PathParam("object") String objectPath) {
        String objectName = URLDecoder.decode(objectPath, StandardCharsets.UTF_8);
        if (!service.deleteObject(bucket, objectName)) {
            throw GcpException.notFound("Object not found: " + objectName);
        }
        return Response.noContent().build();
    }

    @POST
    @Path("/{srcObject: .+}/copyTo/b/{dstBucket}/o/{dstObject: .+}")
    public Response copyObject(
            @PathParam("bucket") String srcBucket,
            @PathParam("srcObject") String srcObjectPath,
            @PathParam("dstBucket") String dstBucket,
            @PathParam("dstObject") String dstObjectPath,
            @Context HttpHeaders headers) {
        String srcObject = URLDecoder.decode(srcObjectPath, StandardCharsets.UTF_8);
        String dstObject = URLDecoder.decode(dstObjectPath, StandardCharsets.UTF_8);
        GcsObjectMeta meta = service.copyObject(srcBucket, srcObject, dstBucket, dstObject, requestBaseUrl(headers));
        return Response.ok(meta).build();
    }

    @POST
    @Path("/{srcObject: .+}/rewriteTo/b/{dstBucket}/o/{dstObject: .+}")
    public Response rewriteObject(
            @PathParam("bucket") String srcBucket,
            @PathParam("srcObject") String srcObjectPath,
            @PathParam("dstBucket") String dstBucket,
            @PathParam("dstObject") String dstObjectPath,
            @Context HttpHeaders headers) {
        String srcObject = URLDecoder.decode(srcObjectPath, StandardCharsets.UTF_8);
        String dstObject = URLDecoder.decode(dstObjectPath, StandardCharsets.UTF_8);
        GcsObjectMeta meta = service.copyObject(srcBucket, srcObject, dstBucket, dstObject, requestBaseUrl(headers));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kind", "storage#rewriteResponse");
        response.put("totalBytesRewritten", meta.getSize());
        response.put("objectSize", meta.getSize());
        response.put("done", true);
        response.put("resource", meta);
        return Response.ok(response).build();
    }

    private String requestBaseUrl(HttpHeaders headers) {
        String host = headers.getHeaderString("Host");
        return host != null ? "http://" + host : config.baseUrl();
    }
}
