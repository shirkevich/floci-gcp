# Cloud Run

floci-gcp emulates the Cloud Run Admin API v2 control plane over REST JSON using Google's published protobuf types.

| Config | Default | Description |
|---|---|---|
| `FLOCI_GCP_SERVICES_CLOUDRUN_ENABLED` | `true` | Enable/disable Cloud Run |

## Supported API Surface

| Operation | Path |
|---|---|
| Create service | `POST /v2/projects/{project}/locations/{location}/services` |
| List services | `GET /v2/projects/{project}/locations/{location}/services` |
| Get service | `GET /v2/projects/{project}/locations/{location}/services/{service}` |
| Delete service | `DELETE /v2/projects/{project}/locations/{location}/services/{service}` |
| Get IAM policy | `GET /v2/projects/{project}/locations/{location}/services/{service}:getIamPolicy` |
| Set IAM policy | `POST /v2/projects/{project}/locations/{location}/services/{service}:setIamPolicy` |
| Test IAM permissions | `POST /v2/projects/{project}/locations/{location}/services/{service}:testIamPermissions` |
| List revisions | `GET /v2/projects/{project}/locations/{location}/services/{service}/revisions` |
| Get revision | `GET /v2/projects/{project}/locations/{location}/services/{service}/revisions/{revision}` |

Create and delete return completed `google.longrunning.Operation` resources immediately. Operations can be read, listed, waited on, and deleted under `/v2/projects/{project}/locations/{location}/operations`.

## Behavior

Cloud Run services are metadata only. Creating a service synthesizes the service URL, timestamps, etag, ready condition, traffic status, latest revision fields, and one read-only revision. No container image is pulled and no request-serving runtime is started.

`validateOnly=true` returns a successful completed operation without storing or deleting resources. Validate-only operations are not retained for later operation get/list calls.

## SDK Usage

Cloud Run clients should use the HTTP JSON transport, an explicit endpoint, and no credentials:

```java
ServicesSettings settings = ServicesSettings.newHttpJsonBuilder()
    .setEndpoint("http://localhost:4588")
    .setCredentialsProvider(NoCredentialsProvider.create())
    .build();
```

## Not Implemented

- Runtime invocation
- Jobs
- WorkerPools
- Service updates
