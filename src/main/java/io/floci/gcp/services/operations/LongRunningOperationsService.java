package io.floci.gcp.services.operations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.longrunning.ListOperationsResponse;
import com.google.longrunning.Operation;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.floci.gcp.core.common.GcpException;
import io.floci.gcp.core.common.PageToken;
import io.floci.gcp.core.common.ProtoJson;
import io.floci.gcp.core.storage.StorageBackend;
import io.floci.gcp.core.storage.StorageFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class LongRunningOperationsService {

    private final StorageBackend<String, String> operationStore;

    @Inject
    public LongRunningOperationsService(StorageFactory storageFactory) {
        this.operationStore = storageFactory.create("operations", "operations.json",
                new TypeReference<Map<String, String>>() {});
    }

    LongRunningOperationsService(StorageBackend<String, String> operationStore) {
        this.operationStore = operationStore;
    }

    public Operation done(String parent, Message response, Message metadata) {
        Operation.Builder builder = Operation.newBuilder()
                .setName(parent + "/operations/" + UUID.randomUUID())
                .setDone(true);
        if (response != null) {
            builder.setResponse(Any.pack(response));
        }
        if (metadata != null) {
            builder.setMetadata(Any.pack(metadata));
        }
        Operation operation = builder.build();
        operationStore.put(operation.getName(), ProtoJson.print(operation));
        return operation;
    }

    public Operation get(String name) {
        return operationStore.get(name)
                .map(this::parse)
                .orElseThrow(() -> GcpException.notFound("Operation not found: " + name));
    }

    public ListOperationsResponse list(String parent, int pageSize, String pageToken) {
        String prefix = parent + "/operations/";
        List<Operation> operations = operationStore.scan(k -> k.startsWith(prefix)).stream()
                .map(this::parse)
                .sorted(Comparator.comparing(Operation::getName))
                .toList();
        PageToken.Page<Operation> page = PageToken.paginate(operations, pageSize, pageToken);
        ListOperationsResponse.Builder response = ListOperationsResponse.newBuilder()
                .addAllOperations(page.items());
        if (page.nextPageToken() != null) {
            response.setNextPageToken(page.nextPageToken());
        }
        return response.build();
    }

    public void delete(String name) {
        operationStore.delete(name);
    }

    private Operation parse(String json) {
        return ProtoJson.merge(json, Operation.newBuilder()).build();
    }
}
