package io.floci.gcp.services.iam;

import io.floci.gcp.services.iam.model.StoredPolicy;
import io.floci.gcp.services.iam.model.StoredServiceAccount;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the IAM v1 API.
 *
 * Routes POST/GET/DELETE /v1/projects/{project}/serviceAccounts[/{email}[:{method}]]
 * Uses JSON transport; disambiguated from DatastoreHttpController by Content-Type.
 */
@Path("/v1/projects")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IamController {

    private static final Logger LOG = Logger.getLogger(IamController.class);

    private static final List<String> CUSTOM_METHODS =
            List.of("getIamPolicy", "setIamPolicy", "testIamPermissions");

    @Inject
    IamService service;

    @POST
    @Path("/{rest:.*}")
    public Response post(@PathParam("rest") String rest, Map<String, Object> body) {
        IamPath p = parsePath(rest);
        LOG.debugf("IAM POST %s project=%s id=%s method=%s", rest, p.project(), p.identifier(), p.customMethod());

        if (p.customMethod() != null) {
            return handleCustomMethod(p, body);
        }

        if ("serviceAccounts".equals(p.resourceType()) && p.identifier() == null) {
            return createServiceAccount(p.project(), body);
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/{rest:.*}")
    public Response get(@PathParam("rest") String rest) {
        IamPath p = parsePath(rest);
        LOG.debugf("IAM GET %s project=%s id=%s", rest, p.project(), p.identifier());

        if (!"serviceAccounts".equals(p.resourceType())) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (p.identifier() == null) {
            List<StoredServiceAccount> accounts = service.listServiceAccounts(p.project());
            return Response.ok(Map.of("accounts", accounts)).build();
        }

        return Response.ok(service.getServiceAccount(p.project(), p.identifier())).build();
    }

    @DELETE
    @Path("/{rest:.*}")
    public Response delete(@PathParam("rest") String rest) {
        IamPath p = parsePath(rest);
        LOG.debugf("IAM DELETE %s project=%s id=%s", rest, p.project(), p.identifier());

        if (!"serviceAccounts".equals(p.resourceType()) || p.identifier() == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        service.deleteServiceAccount(p.project(), p.identifier());
        return Response.ok(Map.of()).build();
    }

    // ── Routing helpers ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Response handleCustomMethod(IamPath p, Map<String, Object> body) {
        String resource = "projects/" + p.project() + "/serviceAccounts/" + p.identifier();
        return switch (p.customMethod()) {
            case "getIamPolicy" -> {
                StoredPolicy policy = service.getPolicy(resource);
                yield Response.ok(policy).build();
            }
            case "setIamPolicy" -> {
                Map<String, Object> policyMap = body != null ? (Map<String, Object>) body.get("policy") : null;
                StoredPolicy policy = parsePolicy(policyMap);
                yield Response.ok(service.setPolicy(resource, policy)).build();
            }
            case "testIamPermissions" -> {
                List<String> requested = body != null ? (List<String>) body.get("permissions") : List.of();
                List<String> granted = service.testPermissions(requested != null ? requested : List.of());
                yield Response.ok(Map.of("permissions", granted)).build();
            }
            default -> Response.status(Response.Status.NOT_FOUND).build();
        };
    }

    @SuppressWarnings("unchecked")
    private Response createServiceAccount(String project, Map<String, Object> body) {
        String accountId = body != null ? (String) body.get("accountId") : null;
        if (accountId == null || accountId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", Map.of("code", 400, "message", "accountId is required", "status", "INVALID_ARGUMENT")))
                    .build();
        }
        Map<String, Object> saProps = body.containsKey("serviceAccount")
                ? (Map<String, Object>) body.get("serviceAccount") : Map.of();
        String displayName = (String) saProps.get("displayName");
        String description = (String) saProps.get("description");
        StoredServiceAccount sa = service.createServiceAccount(project, accountId, displayName, description);
        return Response.ok(sa).build();
    }

    @SuppressWarnings("unchecked")
    private static StoredPolicy parsePolicy(Map<String, Object> policyMap) {
        StoredPolicy policy = new StoredPolicy();
        if (policyMap == null) {
            return policy;
        }
        if (policyMap.containsKey("version")) {
            policy.setVersion(((Number) policyMap.get("version")).intValue());
        }
        if (policyMap.containsKey("bindings")) {
            policy.setBindings((List<Map<String, Object>>) policyMap.get("bindings"));
        }
        if (policyMap.containsKey("etag")) {
            policy.setEtag((String) policyMap.get("etag"));
        }
        return policy;
    }

    // ── Path parsing ───────────────────────────────────────────────────────────

    private static IamPath parsePath(String rest) {
        String customMethod = null;
        String pathPart = rest;
        for (String method : CUSTOM_METHODS) {
            if (rest.endsWith(":" + method)) {
                customMethod = method;
                pathPart = rest.substring(0, rest.length() - method.length() - 1);
                break;
            }
        }
        String[] parts = pathPart.split("/", 3);
        String project = parts.length > 0 ? parts[0] : null;
        String resourceType = parts.length > 1 ? parts[1] : null;
        String identifier = parts.length > 2 ? parts[2] : null;
        return new IamPath(project, resourceType, identifier, customMethod);
    }

    private record IamPath(String project, String resourceType, String identifier, String customMethod) {}
}
