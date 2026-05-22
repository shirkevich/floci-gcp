package io.floci.gcp;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IamIntegrationTest {

    private static final String PROJECT = "test-project";
    private static final String ACCOUNT_ID = "test-sa-" + System.currentTimeMillis();
    private static String email;

    @BeforeAll
    static void init() {
        email = ACCOUNT_ID + "@" + PROJECT + ".iam.gserviceaccount.com";
    }

    @Test
    @Order(1)
    void createServiceAccount() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "accountId", ACCOUNT_ID,
                        "serviceAccount", Map.of("displayName", "Test SA")))
                .when()
                .post("/v1/projects/{project}/serviceAccounts", PROJECT)
                .then()
                .statusCode(200)
                .body("email", equalTo(email))
                .body("projectId", equalTo(PROJECT))
                .body("name", containsString(ACCOUNT_ID));
    }

    @Test
    @Order(2)
    void getServiceAccount() {
        given()
                .when()
                .get("/v1/projects/{project}/serviceAccounts/{email}", PROJECT, email)
                .then()
                .statusCode(200)
                .body("email", equalTo(email))
                .body("displayName", equalTo("Test SA"));
    }

    @Test
    @Order(3)
    void listServiceAccounts() {
        given()
                .when()
                .get("/v1/projects/{project}/serviceAccounts", PROJECT)
                .then()
                .statusCode(200)
                .body("accounts", hasSize(greaterThanOrEqualTo(1)))
                .body("accounts.email", hasItem(email));
    }

    @Test
    @Order(4)
    void getIamPolicy() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of())
                .when()
                .post("/v1/projects/{project}/serviceAccounts/{email}:getIamPolicy", PROJECT, email)
                .then()
                .statusCode(200)
                .body("version", equalTo(1))
                .body("bindings", empty());
    }

    @Test
    @Order(5)
    void setIamPolicy() {
        Map<String, Object> binding = Map.of(
                "role", "roles/iam.serviceAccountUser",
                "members", List.of("user:test@example.com"));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("policy", Map.of(
                        "version", 1,
                        "bindings", List.of(binding))))
                .when()
                .post("/v1/projects/{project}/serviceAccounts/{email}:setIamPolicy", PROJECT, email)
                .then()
                .statusCode(200)
                .body("bindings", hasSize(1))
                .body("bindings[0].role", equalTo("roles/iam.serviceAccountUser"));
    }

    @Test
    @Order(6)
    void testIamPermissions() {
        List<String> permissions = List.of("iam.serviceAccounts.get", "iam.serviceAccounts.list");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("permissions", permissions))
                .when()
                .post("/v1/projects/{project}/serviceAccounts/{email}:testIamPermissions", PROJECT, email)
                .then()
                .statusCode(200)
                .body("permissions", containsInAnyOrder("iam.serviceAccounts.get", "iam.serviceAccounts.list"));
    }

    @Test
    @Order(7)
    void deleteServiceAccount() {
        given()
                .when()
                .delete("/v1/projects/{project}/serviceAccounts/{email}", PROJECT, email)
                .then()
                .statusCode(200);

        given()
                .when()
                .get("/v1/projects/{project}/serviceAccounts", PROJECT)
                .then()
                .statusCode(200)
                .body("accounts.email", not(hasItem(email)));
    }
}
