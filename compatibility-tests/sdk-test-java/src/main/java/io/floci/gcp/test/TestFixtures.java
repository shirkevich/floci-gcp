package io.floci.gcp.test;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretManagerServiceSettings;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static String projectId() {
        return System.getenv().getOrDefault("FLOCI_GCP_PROJECT", "test-project");
    }

    public static String endpoint() {
        return System.getenv().getOrDefault("FLOCI_GCP_ENDPOINT", "http://localhost:4588");
    }

    public static String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Creates a GCS Storage client.
     * The STORAGE_EMULATOR_HOST env var is auto-detected by the GCP SDK.
     * We also explicitly set the host and use NoCredentials for emulator use.
     */
    public static Storage storageClient() {
        return StorageOptions.newBuilder()
                .setHost(endpoint())
                .setProjectId(projectId())
                .setCredentials(NoCredentials.getInstance())
                .build()
                .getService();
    }

    /**
     * Creates a Firestore client.
     * The FIRESTORE_EMULATOR_HOST env var is auto-detected by the GCP SDK.
     */
    public static Firestore firestoreClient() {
        return FirestoreOptions.newBuilder()
                .setProjectId(projectId())
                .setCredentials(NoCredentials.getInstance())
                .build()
                .getService();
    }

    /**
     * Creates a Datastore client.
     * The DATASTORE_EMULATOR_HOST env var is auto-detected by the GCP SDK.
     */
    public static Datastore datastoreClient() {
        return DatastoreOptions.newBuilder()
                .setProjectId(projectId())
                .setCredentials(NoCredentials.getInstance())
                .build()
                .getService();
    }

    /**
     * Creates a Secret Manager client using a plaintext gRPC channel to the emulator.
     * No emulator env var is auto-detected for Secret Manager, so we configure manually.
     */
    public static SecretManagerServiceClient secretManagerClient() throws IOException {
        URI uri = URI.create(endpoint());
        String host = uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : 4588;

        SecretManagerServiceSettings settings = SecretManagerServiceSettings.newBuilder()
                .setTransportChannelProvider(
                        InstantiatingGrpcChannelProvider.newBuilder()
                                .setEndpoint(host + ":" + port)
                                .setChannelConfigurator(builder -> builder.usePlaintext())
                                .build())
                .setCredentialsProvider(NoCredentialsProvider.create())
                .build();

        return SecretManagerServiceClient.create(settings);
    }
}
