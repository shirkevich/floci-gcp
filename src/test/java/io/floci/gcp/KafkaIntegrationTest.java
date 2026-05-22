package io.floci.gcp;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KafkaIntegrationTest {

    private static final String PROJECT = "test-project";
    private static final String LOCATION = "us-central1";
    private static final String CLUSTER_ID = "test-cluster-" + System.currentTimeMillis();
    private static final String TOPIC_ID = "test-topic-" + System.currentTimeMillis();
    private static final String BASE = "/v1/projects/" + PROJECT + "/locations/" + LOCATION;

    @Test
    @Order(1)
    void createCluster() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "capacityConfig", Map.of("vcpuCount", 3, "memoryBytes", 3221225472L),
                        "gcpConfig", Map.of("accessConfig", Map.of("networkConfigs",
                                java.util.List.of(Map.of("subnet", "projects/test/regions/us-central1/subnetworks/default"))))))
                .queryParam("clusterId", CLUSTER_ID)
                .when()
                .post(BASE + "/clusters")
                .then()
                .statusCode(200)
                .body("done", equalTo(true))
                .body("response.name", containsString(CLUSTER_ID))
                .body("response.state", equalTo("ACTIVE"));
    }

    @Test
    @Order(2)
    void getCluster() {
        given()
                .when()
                .get(BASE + "/clusters/" + CLUSTER_ID)
                .then()
                .statusCode(200)
                .body("name", containsString(CLUSTER_ID))
                .body("state", equalTo("ACTIVE"))
                .body("bootstrapAddress", notNullValue());
    }

    @Test
    @Order(3)
    void listClusters() {
        given()
                .when()
                .get(BASE + "/clusters")
                .then()
                .statusCode(200)
                .body("clusters", hasSize(greaterThanOrEqualTo(1)))
                .body("clusters.name", hasItem(containsString(CLUSTER_ID)));
    }

    @Test
    @Order(4)
    void updateCluster() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("capacityConfig", Map.of("vcpuCount", 6, "memoryBytes", 6442450944L)))
                .when()
                .patch(BASE + "/clusters/" + CLUSTER_ID)
                .then()
                .statusCode(200)
                .body("response.vcpuCount", equalTo(6));
    }

    @Test
    @Order(5)
    void createTopic() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("partitionCount", 3, "replicationFactor", 1))
                .queryParam("topicId", TOPIC_ID)
                .when()
                .post(BASE + "/clusters/" + CLUSTER_ID + "/topics")
                .then()
                .statusCode(200)
                .body("name", containsString(TOPIC_ID))
                .body("partitionCount", equalTo(3));
    }

    @Test
    @Order(6)
    void getTopic() {
        given()
                .when()
                .get(BASE + "/clusters/" + CLUSTER_ID + "/topics/" + TOPIC_ID)
                .then()
                .statusCode(200)
                .body("name", containsString(TOPIC_ID))
                .body("partitionCount", equalTo(3));
    }

    @Test
    @Order(7)
    void listTopics() {
        given()
                .when()
                .get(BASE + "/clusters/" + CLUSTER_ID + "/topics")
                .then()
                .statusCode(200)
                .body("topics", hasSize(greaterThanOrEqualTo(1)))
                .body("topics.name", hasItem(containsString(TOPIC_ID)));
    }

    @Test
    @Order(8)
    void updateTopic() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("partitionCount", 6))
                .when()
                .patch(BASE + "/clusters/" + CLUSTER_ID + "/topics/" + TOPIC_ID)
                .then()
                .statusCode(200)
                .body("partitionCount", equalTo(6));
    }

    @Test
    @Order(9)
    void listConsumerGroups() {
        given()
                .when()
                .get(BASE + "/clusters/" + CLUSTER_ID + "/consumerGroups")
                .then()
                .statusCode(200)
                .body("consumerGroups", notNullValue());
    }

    @Test
    @Order(10)
    void deleteTopic() {
        given()
                .when()
                .delete(BASE + "/clusters/" + CLUSTER_ID + "/topics/" + TOPIC_ID)
                .then()
                .statusCode(200);

        given()
                .when()
                .get(BASE + "/clusters/" + CLUSTER_ID + "/topics")
                .then()
                .statusCode(200)
                .body("topics.name", not(hasItem(containsString(TOPIC_ID))));
    }

    @Test
    @Order(11)
    void deleteCluster() {
        given()
                .when()
                .delete(BASE + "/clusters/" + CLUSTER_ID)
                .then()
                .statusCode(200)
                .body("done", equalTo(true));

        given()
                .when()
                .get(BASE + "/clusters")
                .then()
                .statusCode(200)
                .body("clusters.name", not(hasItem(containsString(CLUSTER_ID))));
    }
}
