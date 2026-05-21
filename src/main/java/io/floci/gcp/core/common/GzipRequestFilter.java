package io.floci.gcp.core.common;

import io.quarkus.vertx.http.HttpServerOptionsCustomizer;
import io.vertx.core.http.HttpServerOptions;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Enables Vert.x server-side HTTP decompression so gzip-encoded request bodies
 * (Content-Encoding: gzip) are decompressed before they reach JAX-RS.
 * The Google Cloud Java SDK compresses request bodies by default.
 */
@ApplicationScoped
public class GzipRequestFilter implements HttpServerOptionsCustomizer {

    @Override
    public void customizeHttpServer(HttpServerOptions options) {
        options.setDecompressionSupported(true);
    }
}
