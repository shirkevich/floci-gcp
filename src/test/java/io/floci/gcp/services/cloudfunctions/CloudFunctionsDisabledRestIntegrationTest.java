package io.floci.gcp.services.cloudfunctions;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestProfile(CloudFunctionsDisabledRestIntegrationTest.DisabledCloudFunctionsProfile.class)
class CloudFunctionsDisabledRestIntegrationTest {

    @Test
    void disabledCloudFunctionsServiceReturnsUnavailableWrapper() {
        given()
                .when().get("/v2/projects/functions-disabled/locations/us-central1/functions")
                .then()
                .statusCode(503)
                .body("error.status", equalTo("UNAVAILABLE"))
                .body("error.message", equalTo("Service cloudfunctions is not enabled."));
    }

    public static class DisabledCloudFunctionsProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("floci-gcp.services.cloudfunctions.enabled", "false");
        }
    }
}
