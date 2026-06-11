package io.floci.gcp.services.cloudrun;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestProfile(CloudRunDisabledRestIntegrationTest.DisabledCloudRunProfile.class)
class CloudRunDisabledRestIntegrationTest {

    @Test
    void disabledCloudRunServiceReturnsUnavailableWrapper() {
        given()
                .when().get("/v2/projects/run-disabled/locations/us-central1/services")
                .then()
                .statusCode(503)
                .body("error.status", equalTo("UNAVAILABLE"))
                .body("error.message", equalTo("Service cloudrun is not enabled."));
    }

    public static class DisabledCloudRunProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("floci-gcp.services.cloudrun.enabled", "false");
        }
    }
}
