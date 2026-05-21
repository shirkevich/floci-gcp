package io.floci.gcp;

import com.google.cloud.NoCredentials;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatastoreIntegrationTest {

    private static final String KIND = "IntTestEntity";
    private static final String ENTITY_NAME = "entity-" + UUID.randomUUID().toString().substring(0, 8);

    private static Datastore datastore;
    private static Key entityKey;

    @BeforeAll
    static void setUp() {
        // SDK v2.25.2 uses HttpDatastoreRpc only; setHost("localhost:8081") routes
        // to our emulator at http://localhost:8081/v1/projects/{projectId}:{method}.
        datastore = DatastoreOptions.newBuilder()
                .setProjectId("test-project")
                .setHost("localhost:8081")
                .setCredentials(NoCredentials.getInstance())
                .build()
                .getService();
        KeyFactory keyFactory = datastore.newKeyFactory().setKind(KIND);
        entityKey = keyFactory.newKey(ENTITY_NAME);
    }

    @AfterAll
    static void tearDown() {
        if (datastore != null && entityKey != null) {
            try {
                datastore.delete(entityKey);
            } catch (Exception ignored) {}
        }
    }

    @Test
    @Order(1)
    void putEntity() {
        Entity entity = Entity.newBuilder(entityKey)
                .set("name", "Alice")
                .set("score", 42L)
                .set("active", true)
                .build();

        datastore.put(entity);

        Entity retrieved = datastore.get(entityKey);
        assertNotNull(retrieved);
        assertEquals("Alice", retrieved.getString("name"));
    }

    @Test
    @Order(2)
    void getEntityAndVerifyProperties() {
        Entity entity = datastore.get(entityKey);

        assertNotNull(entity);
        assertEquals("Alice", entity.getString("name"));
        assertEquals(42L, entity.getLong("score"));
        assertTrue(entity.getBoolean("active"));
    }

    @Test
    @Order(3)
    void updateEntityProperty() {
        Entity existing = datastore.get(entityKey);
        assertNotNull(existing);

        Entity updated = Entity.newBuilder(existing)
                .set("score", 99L)
                .set("city", "Testville")
                .build();

        datastore.update(updated);

        Entity retrieved = datastore.get(entityKey);
        assertEquals(99L, retrieved.getLong("score"));
        assertEquals("Testville", retrieved.getString("city"));
        assertEquals("Alice", retrieved.getString("name"));
    }

    @Test
    @Order(4)
    void queryEntitiesWithFilter() {
        Key secondKey = datastore.newKeyFactory().setKind(KIND)
                .newKey("entity-" + UUID.randomUUID().toString().substring(0, 8));
        Entity secondEntity = Entity.newBuilder(secondKey)
                .set("name", "Bob")
                .set("score", 10L)
                .set("active", false)
                .build();
        datastore.put(secondEntity);

        try {
            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind(KIND)
                    .setFilter(PropertyFilter.eq("active", true))
                    .build();

            QueryResults<Entity> results = datastore.run(query);
            List<String> names = new ArrayList<>();
            while (results.hasNext()) {
                names.add(results.next().getString("name"));
            }

            assertTrue(names.contains("Alice"));
            assertFalse(names.contains("Bob"));
        } finally {
            datastore.delete(secondKey);
        }
    }

    @Test
    @Order(5)
    void deleteEntity() {
        datastore.delete(entityKey);

        Entity retrieved = datastore.get(entityKey);
        assertNull(retrieved);
    }
}
