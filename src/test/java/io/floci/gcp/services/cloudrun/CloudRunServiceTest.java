package io.floci.gcp.services.cloudrun;

import com.google.cloud.run.v2.Condition;
import com.google.cloud.run.v2.ListRevisionsResponse;
import com.google.cloud.run.v2.ListServicesResponse;
import com.google.cloud.run.v2.Revision;
import com.google.cloud.run.v2.Service;
import com.google.iam.v1.Binding;
import com.google.iam.v1.Policy;
import com.google.longrunning.Operation;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.floci.gcp.core.common.GcpException;
import io.floci.gcp.core.storage.InMemoryStorage;
import io.floci.gcp.services.iam.IamService;
import io.floci.gcp.services.iam.model.StoredPolicy;
import io.floci.gcp.services.operations.LongRunningOperationsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CloudRunServiceTest {

    private CloudRunService service;
    private IamService iamService;

    @BeforeEach
    void setUp() {
        LongRunningOperationsService operations = mock(LongRunningOperationsService.class);
        when(operations.done(anyString(), any(Message.class), any(Message.class)))
                .thenAnswer(invocation -> Operation.newBuilder()
                        .setName(invocation.getArgument(0, String.class) + "/operations/test-op")
                        .setDone(true)
                        .setResponse(Any.pack(invocation.getArgument(1, Message.class)))
                        .setMetadata(Any.pack(invocation.getArgument(2, Message.class)))
                        .build());
        when(operations.doneTransient(anyString(), any(Message.class), any(Message.class)))
                .thenAnswer(invocation -> Operation.newBuilder()
                        .setName(invocation.getArgument(0, String.class) + "/operations/test-op")
                        .setDone(true)
                        .setResponse(Any.pack(invocation.getArgument(1, Message.class)))
                        .setMetadata(Any.pack(invocation.getArgument(2, Message.class)))
                        .build());
        iamService = mock(IamService.class);
        service = new CloudRunService(new InMemoryStorage<>(), new InMemoryStorage<>(), operations, iamService);
    }

    @Test
    void createSynthesizesServiceAndRevision() {
        Operation operation = service.createService("p1", "us-central1", "svc",
                "{\"template\":{\"containers\":[{\"image\":\"gcr.io/p1/svc:latest\"}]}}", false);

        assertTrue(operation.getDone());

        Service created = service.getService("projects/p1/locations/us-central1/services/svc");
        assertEquals("projects/p1/locations/us-central1/services/svc", created.getName());
        assertTrue(created.getUri().startsWith("https://svc-"));
        assertEquals(created.getLatestCreatedRevision(), created.getLatestReadyRevision());
        assertEquals(Condition.State.CONDITION_SUCCEEDED, created.getTerminalCondition().getState());
        assertEquals(1, created.getTrafficStatusesCount());
        assertEquals(100, created.getTrafficStatuses(0).getPercent());

        Revision revision = service.getRevision(created.getLatestReadyRevision());
        assertEquals(created.getName(), revision.getService());
        assertEquals("gcr.io/p1/svc:latest", revision.getContainers(0).getImage());
    }

    @Test
    void duplicateCreateAndMissingGetUseGcpErrors() {
        service.createService("p1", "us-central1", "svc", "{}", false);

        GcpException duplicate = assertThrows(GcpException.class,
                () -> service.createService("p1", "us-central1", "svc", "{}", false));
        assertEquals("ALREADY_EXISTS", duplicate.getGcpStatus());

        GcpException missing = assertThrows(GcpException.class,
                () -> service.getService("projects/p1/locations/us-central1/services/missing"));
        assertEquals("NOT_FOUND", missing.getGcpStatus());
    }

    @Test
    void listServicesPaginatesAndFiltersByProjectAndLocation() {
        service.createService("p1", "us-central1", "a", "{}", false);
        service.createService("p1", "us-central1", "b", "{}", false);
        service.createService("p2", "us-central1", "a", "{}", false);
        service.createService("p1", "europe-west1", "c", "{}", false);

        ListServicesResponse firstPage = service.listServices("p1", "us-central1", 1, null);
        assertEquals(1, firstPage.getServicesCount());
        assertFalse(firstPage.getNextPageToken().isBlank());

        ListServicesResponse secondPage = service.listServices("p1", "us-central1", 10,
                firstPage.getNextPageToken());
        assertEquals(1, secondPage.getServicesCount());
        assertEquals("", secondPage.getNextPageToken());

        assertEquals(1, service.listServices("p2", "us-central1", 10, null).getServicesCount());
        assertEquals(1, service.listServices("p1", "europe-west1", 10, null).getServicesCount());
    }

    @Test
    void validateOnlyDoesNotPersistCreateOrDeleteMutations() {
        service.createService("p1", "us-central1", "validate", "{}", true);
        assertThrows(GcpException.class,
                () -> service.getService("projects/p1/locations/us-central1/services/validate"));

        service.createService("p1", "us-central1", "real", "{}", false);
        service.deleteService("projects/p1/locations/us-central1/services/real", true);

        assertEquals("projects/p1/locations/us-central1/services/real",
                service.getService("projects/p1/locations/us-central1/services/real").getName());
    }

    @Test
    void deleteRemovesServiceAndReadOnlyRevision() {
        service.createService("p1", "us-central1", "svc", "{}", false);
        String name = "projects/p1/locations/us-central1/services/svc";
        String revision = service.getService(name).getLatestReadyRevision();

        Operation operation = service.deleteService(name, false);

        assertTrue(operation.getDone());
        assertThrows(GcpException.class, () -> service.getService(name));
        assertThrows(GcpException.class, () -> service.getRevision(revision));
    }

    @Test
    void listRevisionsPaginatesAndRequiresParentService() {
        service.createService("p1", "us-central1", "svc", "{}", false);

        ListRevisionsResponse revisions = service.listRevisions(
                "projects/p1/locations/us-central1/services/svc", 1, null);

        assertEquals(1, revisions.getRevisionsCount());
        assertEquals("projects/p1/locations/us-central1/services/svc", revisions.getRevisions(0).getService());
        assertThrows(GcpException.class, () -> service.listRevisions(
                "projects/p1/locations/us-central1/services/missing", 1, null));
    }

    @Test
    void iamPolicyConversionsDelegateToIamService() {
        String resource = "projects/p1/locations/us-central1/services/svc";
        StoredPolicy stored = new StoredPolicy();
        stored.setVersion(3);
        stored.setBindings(List.of());
        when(iamService.getPolicy(resource)).thenReturn(stored);

        assertEquals(3, service.getIamPolicy(resource).getVersion());

        Policy requested = Policy.newBuilder()
                .setVersion(1)
                .addBindings(Binding.newBuilder()
                        .setRole("roles/run.invoker")
                        .addMembers("allUsers")
                        .build())
                .build();
        when(iamService.setPolicy(eq(resource), any(StoredPolicy.class))).thenAnswer(invocation -> invocation.getArgument(1));

        Policy saved = service.setIamPolicy(resource, requested);

        assertEquals("roles/run.invoker", saved.getBindings(0).getRole());
        assertEquals(List.of("allUsers"), saved.getBindings(0).getMembersList());
    }
}
