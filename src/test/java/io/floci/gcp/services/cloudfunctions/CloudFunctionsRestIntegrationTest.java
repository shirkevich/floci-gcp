package io.floci.gcp.services.cloudfunctions;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class CloudFunctionsRestIntegrationTest {

    @Test
    void cloudFunctionsRestPathsUseProtoJsonUploadUrlAndLroPolling() {
        String project = "functions-it-1";
        String location = "us-central1";
        String functionId = "fn";
        String functionPath = "/v2/projects/" + project + "/locations/" + location + "/functions/" + functionId;

        String operationName = given()
                .contentType("application/json")
                .queryParam("functionId", functionId)
                .body("{\"buildConfig\":{\"runtime\":\"java21\"}}")
                .when().post("/v2/projects/" + project + "/locations/" + location + "/functions")
                .then()
                .statusCode(200)
                .body("done", equalTo(true))
                .body("response.'@type'", equalTo("type.googleapis.com/google.cloud.functions.v2.Function"))
                .body("response.name", equalTo("projects/" + project + "/locations/" + location + "/functions/" + functionId))
                .body("response.state", equalTo("ACTIVE"))
                .body("response.environment", equalTo("GEN_2"))
                .body("response.serviceConfig.service",
                        equalTo("projects/" + project + "/locations/" + location + "/services/" + functionId))
                .extract().path("name");

        given()
                .when().get(functionPath)
                .then()
                .statusCode(200)
                .body("url", equalTo("https://" + location + "-" + project + ".cloudfunctions.net/" + functionId))
                .body("serviceConfig.allTrafficOnLatestRevision", equalTo(true));

        given()
                .queryParam("$alt", "json;enum-encoding=int")
                .when().get("/v2/projects/" + project + "/locations/" + location + "/functions")
                .then()
                .statusCode(200)
                .body("functions.name", hasItem("projects/" + project + "/locations/" + location + "/functions/" + functionId));

        given()
                .when().get("/v2/" + operationName)
                .then()
                .statusCode(200)
                .body("metadata.operationType", equalTo("CREATE_FUNCTION"))
                .body("response.name", equalTo("projects/" + project + "/locations/" + location + "/functions/" + functionId));

        String uploadUrl = given()
                .urlEncodingEnabled(false)
                .contentType("application/json")
                .body("{}")
                .when().post("/v2/projects/" + project + "/locations/" + location + "/functions:generateUploadUrl")
                .then()
                .statusCode(200)
                .body("uploadUrl", startsWith("http://"))
                .body("storageSource.bucket", startsWith("gcf-v2-sources-functions-it-1-us-central1"))
                .body("storageSource.object", startsWith("source-"))
                .extract().path("uploadUrl");

        given()
                .contentType("application/octet-stream")
                .body("fake zip bytes".getBytes(StandardCharsets.UTF_8))
                .when().put(URI.create(uploadUrl).getRawPath())
                .then()
                .statusCode(200)
                .body("bucket", startsWith("gcf-v2-sources-functions-it-1-us-central1"))
                .body("name", startsWith("source-"));

        given()
                .urlEncodingEnabled(false)
                .contentType("application/json")
                .body("{}")
                .when().post("/v2/" + operationName + ":wait")
                .then()
                .statusCode(200)
                .body("done", equalTo(true));

        String deleteOperation = given()
                .when().delete(functionPath)
                .then()
                .statusCode(200)
                .body("done", equalTo(true))
                .extract().path("name");

        given()
                .when().get("/v2/" + deleteOperation)
                .then()
                .statusCode(200)
                .body("metadata.operationType", equalTo("DELETE_FUNCTION"));

        given()
                .when().get(functionPath)
                .then()
                .statusCode(404)
                .body("error.status", equalTo("NOT_FOUND"));
    }

    @Test
    void validateOnlyCreateDoesNotPersistFunction() {
        String project = "functions-it-validate";
        String location = "us-central1";

        given()
                .contentType("application/json")
                .queryParam("functionId", "fn")
                .queryParam("validateOnly", true)
                .body("{}")
                .when().post("/v2/projects/" + project + "/locations/" + location + "/functions")
                .then()
                .statusCode(200)
                .body("done", equalTo(true));

        given()
                .when().get("/v2/projects/" + project + "/locations/" + location + "/functions/fn")
                .then()
                .statusCode(404)
                .body("error.status", equalTo("NOT_FOUND"));
    }
}
