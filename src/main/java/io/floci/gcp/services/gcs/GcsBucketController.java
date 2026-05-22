package io.floci.gcp.services.gcs;

import io.floci.gcp.config.EmulatorConfig;
import io.floci.gcp.core.common.GcpException;
import io.floci.gcp.services.gcs.model.GcsBucket;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Path("/storage/v1/b")
@Produces(MediaType.APPLICATION_JSON)
public class GcsBucketController {

    private final GcsService service;
    private final EmulatorConfig config;

    @Inject
    public GcsBucketController(GcsService service, EmulatorConfig config) {
        this.service = service;
        this.config = config;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createBucket(@QueryParam("project") String project, @Context HttpHeaders headers, Map<String, Object> body) {
        String name = (String) body.get("name");
        if (name == null || name.isBlank()) {
            throw GcpException.invalidArgument("bucket name is required");
        }
        @SuppressWarnings("unchecked")
        Map<String, String> labels = (Map<String, String>) body.get("labels");
        GcsBucket bucket = service.createBucket(name, project, requestBaseUrl(headers), labels);
        return Response.ok(bucket).build();
    }

    @GET
    public Response listBuckets(@QueryParam("project") String project) {
        List<GcsBucket> items = service.listBuckets(project);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kind", "storage#buckets");
        if (!items.isEmpty()) {
            response.put("items", items);
        }
        return Response.ok(response).build();
    }

    @GET
    @Path("/{bucket}")
    public Response getBucket(@PathParam("bucket") String bucket) {
        return Response.ok(service.getBucket(bucket)).build();
    }

    @PATCH
    @Path("/{bucket}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response patchBucket(@PathParam("bucket") String bucket, Map<String, Object> body) {
        return Response.ok(service.updateBucket(bucket, body)).build();
    }

    @DELETE
    @Path("/{bucket}")
    public Response deleteBucket(@PathParam("bucket") String bucket) {
        service.deleteBucket(bucket);
        return Response.noContent().build();
    }

    private String requestBaseUrl(HttpHeaders headers) {
        String host = headers.getHeaderString("Host");
        return host != null ? "http://" + host : config.baseUrl();
    }
}
