package io.floci.gcp.core.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps {@link GcpException} to the canonical GCP REST error shape:
 * <pre>{"error": {"code": 404, "message": "...", "status": "NOT_FOUND"}}</pre>
 */
@Provider
public class GcpExceptionMapper implements ExceptionMapper<GcpException> {

    @Override
    public Response toResponse(GcpException ex) {
        return Response.status(ex.getHttpStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorWrapper(new ErrorDetail(ex.getHttpStatus(), ex.getMessage(), ex.getGcpStatus())))
                .build();
    }

    @RegisterForReflection
    public record ErrorWrapper(@JsonProperty("error") ErrorDetail error) {}

    @RegisterForReflection
    public record ErrorDetail(@JsonProperty("code") int code,
                              @JsonProperty("message") String message,
                              @JsonProperty("status") String status) {}
}
