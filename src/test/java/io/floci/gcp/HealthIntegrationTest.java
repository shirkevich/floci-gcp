package io.floci.gcp;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class HealthIntegrationTest {

    @Test
    void healthEndpointReturns200() {
        given()
            .when().get("/health")
            .then()
            .statusCode(200)
            .body("services", notNullValue());
    }

    @Test
    void infoEndpointReturnsPortAndProject() {
        given()
            .when().get("/_floci-gcp/info")
            .then()
            .statusCode(200)
            .body("port", equalTo(4588))
            .body("defaultProject", equalTo("test-project"));
    }

    @Test
    void initEndpointReturnsLifecycleState() {
        given()
            .when().get("/_floci-gcp/init")
            .then()
            .statusCode(200)
            .body("completed.boot", equalTo(true));
    }
}
