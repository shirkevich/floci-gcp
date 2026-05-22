package io.floci.gcp.services.iam;

import com.fasterxml.jackson.core.type.TypeReference;
import io.floci.gcp.config.EmulatorConfig;
import io.floci.gcp.core.common.GcpException;
import io.floci.gcp.core.common.ServiceDescriptor;
import io.floci.gcp.core.common.ServiceProtocol;
import io.floci.gcp.core.common.ServiceRegistry;
import io.floci.gcp.core.storage.StorageBackend;
import io.floci.gcp.core.storage.StorageFactory;
import io.floci.gcp.services.iam.model.StoredPolicy;
import io.floci.gcp.services.iam.model.StoredServiceAccount;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class IamService {

    private static final Logger LOG = Logger.getLogger(IamService.class);

    private final StorageBackend<String, StoredServiceAccount> saStore;
    private final StorageBackend<String, StoredPolicy> policyStore;
    private final ServiceRegistry serviceRegistry;
    private final EmulatorConfig config;
    private final AtomicLong uniqueIdSeq = new AtomicLong(100000000000000000L);

    @Inject
    public IamService(ServiceRegistry serviceRegistry, EmulatorConfig config, StorageFactory storageFactory) {
        this.serviceRegistry = serviceRegistry;
        this.config = config;
        this.saStore = storageFactory.createGlobal("iam-service-accounts", "iam-service-accounts.json",
                new TypeReference<Map<String, StoredServiceAccount>>() {});
        this.policyStore = storageFactory.createGlobal("iam-policies", "iam-policies.json",
                new TypeReference<Map<String, StoredPolicy>>() {});
    }

    void onStart(@Observes StartupEvent ev) {
        serviceRegistry.register(ServiceDescriptor.builder("iam")
                .enabled(config.services().iam().enabled())
                .storageKey("iam")
                .protocol(ServiceProtocol.REST)
                .resourceClasses(IamController.class)
                .build());
    }

    // ── Service Accounts ───────────────────────────────────────────────────────

    public StoredServiceAccount createServiceAccount(String project, String accountId,
            String displayName, String description) {
        String email = accountId + "@" + project + ".iam.gserviceaccount.com";
        String key = saKey(project, email);
        if (saStore.get(key).isPresent()) {
            throw GcpException.alreadyExists("Service account already exists: " + email);
        }
        String uniqueId = String.valueOf(uniqueIdSeq.getAndIncrement());
        String etag = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        StoredServiceAccount sa = new StoredServiceAccount(
                "projects/" + project + "/serviceAccounts/" + email,
                project, uniqueId, email,
                displayName != null ? displayName : accountId,
                description,
                Instant.now().toString(),
                etag);
        saStore.put(key, sa);
        LOG.debugf("createServiceAccount project=%s email=%s", project, email);
        return sa;
    }

    public StoredServiceAccount getServiceAccount(String project, String emailOrId) {
        String email = resolveEmail(project, emailOrId);
        return saStore.get(saKey(project, email))
                .orElseThrow(() -> GcpException.notFound("Service account not found: " + email));
    }

    public List<StoredServiceAccount> listServiceAccounts(String project) {
        String prefix = "sa:" + project + ":";
        return saStore.scan(k -> k.startsWith(prefix));
    }

    public void deleteServiceAccount(String project, String emailOrId) {
        String email = resolveEmail(project, emailOrId);
        String key = saKey(project, email);
        saStore.get(key).orElseThrow(() -> GcpException.notFound("Service account not found: " + email));
        saStore.delete(key);
        LOG.debugf("deleteServiceAccount project=%s email=%s", project, email);
    }

    // ── IAM Policies ───────────────────────────────────────────────────────────

    public StoredPolicy getPolicy(String resource) {
        return policyStore.get(policyKey(resource)).orElse(new StoredPolicy());
    }

    public StoredPolicy setPolicy(String resource, StoredPolicy policy) {
        policyStore.put(policyKey(resource), policy);
        return policy;
    }

    public List<String> testPermissions(List<String> permissions) {
        return permissions;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static String resolveEmail(String project, String emailOrId) {
        return emailOrId.contains("@") ? emailOrId : emailOrId + "@" + project + ".iam.gserviceaccount.com";
    }

    private static String saKey(String project, String email) {
        return "sa:" + project + ":" + email;
    }

    private static String policyKey(String resource) {
        return "policy:" + resource;
    }
}
