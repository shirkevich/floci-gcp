package io.floci.gcp.services.cloudrun;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.run.v2.Condition;
import com.google.cloud.run.v2.ListRevisionsResponse;
import com.google.cloud.run.v2.ListServicesResponse;
import com.google.cloud.run.v2.Revision;
import com.google.cloud.run.v2.TrafficTarget;
import com.google.cloud.run.v2.TrafficTargetAllocationType;
import com.google.cloud.run.v2.TrafficTargetStatus;
import com.google.iam.v1.Binding;
import com.google.iam.v1.Policy;
import com.google.iam.v1.TestIamPermissionsResponse;
import com.google.longrunning.Operation;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.google.type.Expr;
import io.floci.gcp.config.EmulatorConfig;
import io.floci.gcp.core.common.GcpException;
import io.floci.gcp.core.common.GcpResourceNames;
import io.floci.gcp.core.common.PageToken;
import io.floci.gcp.core.common.ProtoJson;
import io.floci.gcp.core.common.ServiceDescriptor;
import io.floci.gcp.core.common.ServiceProtocol;
import io.floci.gcp.core.common.ServiceRegistry;
import io.floci.gcp.core.storage.StorageBackend;
import io.floci.gcp.core.storage.StorageFactory;
import io.floci.gcp.services.iam.IamService;
import io.floci.gcp.services.iam.model.StoredPolicy;
import io.floci.gcp.services.operations.LongRunningOperationsService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class CloudRunService {

    private static final Logger LOG = Logger.getLogger(CloudRunService.class);

    private final StorageBackend<String, String> serviceStore;
    private final StorageBackend<String, String> revisionStore;
    private final LongRunningOperationsService operations;
    private final IamService iamService;
    private final ServiceRegistry serviceRegistry;
    private final EmulatorConfig config;

    @Inject
    public CloudRunService(StorageFactory storageFactory,
                           LongRunningOperationsService operations,
                           IamService iamService,
                           ServiceRegistry serviceRegistry,
                           EmulatorConfig config) {
        this.serviceStore = storageFactory.create("cloudrun-services", "cloudrun-services.json",
                new TypeReference<Map<String, String>>() {});
        this.revisionStore = storageFactory.create("cloudrun-revisions", "cloudrun-revisions.json",
                new TypeReference<Map<String, String>>() {});
        this.operations = operations;
        this.iamService = iamService;
        this.serviceRegistry = serviceRegistry;
        this.config = config;
    }

    CloudRunService(StorageBackend<String, String> serviceStore,
                    StorageBackend<String, String> revisionStore,
                    LongRunningOperationsService operations,
                    IamService iamService) {
        this.serviceStore = serviceStore;
        this.revisionStore = revisionStore;
        this.operations = operations;
        this.iamService = iamService;
        this.serviceRegistry = null;
        this.config = null;
    }

    void onStart(@Observes StartupEvent ev) {
        serviceRegistry.register(ServiceDescriptor.builder("cloudrun")
                .enabled(config.services().cloudrun().enabled())
                .storageKey("cloudrun")
                .protocol(ServiceProtocol.REST)
                .resourceClasses(CloudRunController.class)
                .build());
    }

    public Operation createService(String project, String location, String serviceId,
                                   String body, boolean validateOnly) {
        String parent = parent(project, location);
        com.google.cloud.run.v2.Service requested = ProtoJson
                .merge(body, com.google.cloud.run.v2.Service.newBuilder())
                .build();
        String id = firstPresent(serviceId, GcpResourceNames.lastSegment(requested.getName()));
        if (id == null || id.isBlank()) {
            throw GcpException.invalidArgument("serviceId query parameter is required");
        }
        String name = parent + "/services/" + id;
        if (serviceStore.get(name).isPresent()) {
            throw GcpException.alreadyExists("Cloud Run service already exists: " + name);
        }

        Timestamp now = timestampNow();
        String revisionName = name + "/revisions/" + id + "-00001";
        String uri = "https://" + id + "-" + stableSuffix(project, location) + ".a.run.app";

        com.google.cloud.run.v2.Service service = populateService(requested, name, revisionName, uri, now);
        Revision revision = populateRevision(requested, name, revisionName, now);

        if (!validateOnly) {
            serviceStore.put(name, ProtoJson.print(service));
            revisionStore.put(revisionName, ProtoJson.print(revision));
        }

        LOG.infof("create Cloud Run service name=%s validateOnly=%s", name, validateOnly);
        return done(parent, service, service, validateOnly);
    }

    public com.google.cloud.run.v2.Service getService(String name) {
        return serviceStore.get(name)
                .map(json -> ProtoJson.merge(json, com.google.cloud.run.v2.Service.newBuilder()).build())
                .orElseThrow(() -> GcpException.notFound("Cloud Run service not found: " + name));
    }

    public ListServicesResponse listServices(String project, String location, int pageSize, String pageToken) {
        String prefix = parent(project, location) + "/services/";
        List<com.google.cloud.run.v2.Service> services = serviceStore.scan(k -> k.startsWith(prefix)).stream()
                .map(json -> ProtoJson.merge(json, com.google.cloud.run.v2.Service.newBuilder()).build())
                .sorted(Comparator.comparing(com.google.cloud.run.v2.Service::getName))
                .toList();
        PageToken.Page<com.google.cloud.run.v2.Service> page = PageToken.paginate(services, pageSize, pageToken);
        ListServicesResponse.Builder response = ListServicesResponse.newBuilder()
                .addAllServices(page.items());
        if (page.nextPageToken() != null) {
            response.setNextPageToken(page.nextPageToken());
        }
        return response.build();
    }

    public Operation deleteService(String name, boolean validateOnly) {
        com.google.cloud.run.v2.Service existing = getService(name);
        Timestamp now = timestampNow();
        com.google.cloud.run.v2.Service deleted = existing.toBuilder()
                .setDeleteTime(now)
                .setUpdateTime(now)
                .build();
        if (!validateOnly) {
            serviceStore.delete(name);
            String revisionPrefix = name + "/revisions/";
            revisionStore.keys().stream()
                    .filter(k -> k.startsWith(revisionPrefix))
                    .forEach(revisionStore::delete);
        }
        LOG.infof("delete Cloud Run service name=%s validateOnly=%s", name, validateOnly);
        return done(parentFromName(name), deleted, deleted, validateOnly);
    }

    public Revision getRevision(String name) {
        return revisionStore.get(name)
                .map(json -> ProtoJson.merge(json, Revision.newBuilder()).build())
                .orElseThrow(() -> GcpException.notFound("Cloud Run revision not found: " + name));
    }

    public ListRevisionsResponse listRevisions(String serviceName, int pageSize, String pageToken) {
        getService(serviceName);
        String prefix = serviceName + "/revisions/";
        List<Revision> revisions = revisionStore.scan(k -> k.startsWith(prefix)).stream()
                .map(json -> ProtoJson.merge(json, Revision.newBuilder()).build())
                .sorted(Comparator.comparing(Revision::getName))
                .toList();
        PageToken.Page<Revision> page = PageToken.paginate(revisions, pageSize, pageToken);
        ListRevisionsResponse.Builder response = ListRevisionsResponse.newBuilder()
                .addAllRevisions(page.items());
        if (page.nextPageToken() != null) {
            response.setNextPageToken(page.nextPageToken());
        }
        return response.build();
    }

    public Policy getIamPolicy(String resource) {
        return toProtoPolicy(iamService.getPolicy(resource));
    }

    public Policy setIamPolicy(String resource, Policy policy) {
        return toProtoPolicy(iamService.setPolicy(resource, toStoredPolicy(policy)));
    }

    public TestIamPermissionsResponse testIamPermissions(List<String> permissions) {
        return TestIamPermissionsResponse.newBuilder()
                .addAllPermissions(iamService.testPermissions(permissions))
                .build();
    }

    private static com.google.cloud.run.v2.Service populateService(
            com.google.cloud.run.v2.Service requested, String name, String revisionName, String uri, Timestamp now) {
        com.google.cloud.run.v2.Service.Builder builder = requested.toBuilder()
                .setName(name)
                .setUid(UUID.randomUUID().toString())
                .setGeneration(1)
                .setObservedGeneration(1)
                .setCreateTime(now)
                .setUpdateTime(now)
                .setLatestReadyRevision(revisionName)
                .setLatestCreatedRevision(revisionName)
                .setTerminalCondition(readyCondition(now))
                .addConditions(readyCondition(now))
                .setUri(uri)
                .addUrls(uri)
                .setReconciling(false)
                .setEtag(UUID.randomUUID().toString());
        if (builder.getTrafficCount() == 0) {
            builder.addTraffic(TrafficTarget.newBuilder()
                    .setType(TrafficTargetAllocationType.TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST)
                    .setPercent(100)
                    .build());
        }
        builder.clearTrafficStatuses();
        for (TrafficTarget target : builder.getTrafficList()) {
            builder.addTrafficStatuses(TrafficTargetStatus.newBuilder()
                    .setType(target.getType())
                    .setRevision(target.getRevision().isBlank() ? revisionName : target.getRevision())
                    .setPercent(target.getPercent())
                    .setTag(target.getTag())
                    .setUri(uri)
                    .build());
        }
        return builder.build();
    }

    private static Revision populateRevision(com.google.cloud.run.v2.Service requested,
                                             String serviceName, String revisionName, Timestamp now) {
        Revision.Builder builder = Revision.newBuilder()
                .setName(revisionName)
                .setService(serviceName)
                .setUid(UUID.randomUUID().toString())
                .setGeneration(1)
                .setObservedGeneration(1)
                .setCreateTime(now)
                .setUpdateTime(now)
                .addConditions(readyCondition(now))
                .setReconciling(false)
                .setEtag(UUID.randomUUID().toString());
        builder.putAllLabels(requested.getLabelsMap());
        builder.putAllAnnotations(requested.getAnnotationsMap());
        if (requested.hasTemplate()) {
            builder.addAllContainers(requested.getTemplate().getContainersList());
            builder.addAllVolumes(requested.getTemplate().getVolumesList());
            builder.setServiceAccount(requested.getTemplate().getServiceAccount());
            builder.setMaxInstanceRequestConcurrency(requested.getTemplate().getMaxInstanceRequestConcurrency());
            if (requested.getTemplate().hasTimeout()) {
                builder.setTimeout(requested.getTemplate().getTimeout());
            }
        }
        return builder.build();
    }

    private static Condition readyCondition(Timestamp now) {
        return Condition.newBuilder()
                .setType("Ready")
                .setState(Condition.State.CONDITION_SUCCEEDED)
                .setLastTransitionTime(now)
                .build();
    }

    private Operation done(String parent, com.google.cloud.run.v2.Service response,
                           com.google.cloud.run.v2.Service metadata, boolean transientOnly) {
        return transientOnly
                ? operations.doneTransient(parent, response, metadata)
                : operations.done(parent, response, metadata);
    }

    private static StoredPolicy toStoredPolicy(Policy policy) {
        StoredPolicy stored = new StoredPolicy();
        stored.setVersion(policy.getVersion());
        if (!policy.getEtag().isEmpty()) {
            stored.setEtag(policy.getEtag().toStringUtf8());
        }
        stored.setBindings(policy.getBindingsList().stream()
                .map(CloudRunService::bindingToMap)
                .toList());
        return stored;
    }

    private static Policy toProtoPolicy(StoredPolicy stored) {
        Policy.Builder builder = Policy.newBuilder()
                .setVersion(stored.getVersion());
        if (stored.getEtag() != null) {
            builder.setEtag(ByteString.copyFromUtf8(stored.getEtag()));
        }
        if (stored.getBindings() != null) {
            for (Map<String, Object> binding : stored.getBindings()) {
                builder.addBindings(mapToBinding(binding));
            }
        }
        return builder.build();
    }

    private static Map<String, Object> bindingToMap(Binding binding) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("role", binding.getRole());
        map.put("members", List.copyOf(binding.getMembersList()));
        if (binding.hasCondition()) {
            Map<String, Object> condition = new LinkedHashMap<>();
            condition.put("expression", binding.getCondition().getExpression());
            condition.put("title", binding.getCondition().getTitle());
            condition.put("description", binding.getCondition().getDescription());
            condition.put("location", binding.getCondition().getLocation());
            map.put("condition", condition);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Binding mapToBinding(Map<String, Object> map) {
        Binding.Builder builder = Binding.newBuilder()
                .setRole((String) map.getOrDefault("role", ""));
        Object members = map.get("members");
        if (members instanceof List<?> list) {
            for (Object member : list) {
                builder.addMembers(String.valueOf(member));
            }
        }
        Object condition = map.get("condition");
        if (condition instanceof Map<?, ?> raw) {
            Map<String, Object> c = (Map<String, Object>) raw;
            builder.setCondition(Expr.newBuilder()
                    .setExpression((String) c.getOrDefault("expression", ""))
                    .setTitle((String) c.getOrDefault("title", ""))
                    .setDescription((String) c.getOrDefault("description", ""))
                    .setLocation((String) c.getOrDefault("location", ""))
                    .build());
        }
        return builder.build();
    }

    private static String firstPresent(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    private static Timestamp timestampNow() {
        Instant now = Instant.now();
        return Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();
    }

    private static String parent(String project, String location) {
        return "projects/" + project + "/locations/" + location;
    }

    private static String parentFromName(String name) {
        int services = name.indexOf("/services/");
        return services < 0 ? name : name.substring(0, services);
    }

    private static String stableSuffix(String project, String location) {
        return Integer.toHexString((project + ":" + location).hashCode()).replace("-", "0");
    }
}
