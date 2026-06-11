package io.floci.gcp.services.cloudfunctions;

import com.google.cloud.functions.v2.GenerateUploadUrlRequest;
import io.floci.gcp.core.common.ProtoJson;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v2/projects/{project}/locations/{location}")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CloudFunctionsController {

    private final CloudFunctionsService service;

    @Inject
    public CloudFunctionsController(CloudFunctionsService service) {
        this.service = service;
    }

    @POST
    @Path("/functions")
    public Response createFunction(@PathParam("project") String project,
                                   @PathParam("location") String location,
                                   @QueryParam("functionId") String functionId,
                                   @QueryParam("validateOnly") @DefaultValue("false") boolean validateOnly,
                                   String body) {
        return json(ProtoJson.print(service.createFunction(project, location, functionId, body, validateOnly)));
    }

    @GET
    @Path("/functions")
    public Response listFunctions(@PathParam("project") String project,
                                  @PathParam("location") String location,
                                  @QueryParam("pageSize") @DefaultValue("0") int pageSize,
                                  @QueryParam("pageToken") String pageToken) {
        return json(ProtoJson.print(service.listFunctions(project, location, pageSize, pageToken)));
    }

    @GET
    @Path("/functions/{functionId}")
    public Response getFunction(@PathParam("project") String project,
                                @PathParam("location") String location,
                                @PathParam("functionId") String functionId) {
        return json(ProtoJson.print(service.getFunction(functionName(project, location, functionId))));
    }

    @DELETE
    @Path("/functions/{functionId}")
    public Response deleteFunction(@PathParam("project") String project,
                                   @PathParam("location") String location,
                                   @PathParam("functionId") String functionId,
                                   @QueryParam("validateOnly") @DefaultValue("false") boolean validateOnly) {
        return json(ProtoJson.print(service.deleteFunction(functionName(project, location, functionId), validateOnly)));
    }

    @POST
    @Path("/functions:generateUploadUrl")
    public Response generateUploadUrl(@PathParam("project") String project,
                                      @PathParam("location") String location,
                                      @Context HttpHeaders headers,
                                      String body) {
        ProtoJson.merge(body, GenerateUploadUrlRequest.newBuilder()).build();
        return json(ProtoJson.print(service.generateUploadUrl(project, location, requestBaseUrl(headers))));
    }

    private static Response json(String json) {
        return Response.ok(json, MediaType.APPLICATION_JSON).build();
    }

    private static String functionName(String project, String location, String functionId) {
        return "projects/" + project + "/locations/" + location + "/functions/" + functionId;
    }

    private static String requestBaseUrl(HttpHeaders headers) {
        String host = headers.getHeaderString("Host");
        return host != null ? "http://" + host : "http://localhost:4588";
    }
}
