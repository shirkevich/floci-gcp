"""Secret Manager integration tests using google-cloud-secret-manager."""

import pytest


def test_create_secret(secret_manager_client, project_id, unique_name):
    secret_id = f"test-secret-{unique_name}"
    parent = f"projects/{project_id}"

    response = secret_manager_client.create_secret(
        request={
            "parent": parent,
            "secret_id": secret_id,
            "secret": {"replication": {"automatic": {}}},
        }
    )
    assert response.name == f"{parent}/secrets/{secret_id}"

    # Cleanup
    secret_manager_client.delete_secret(request={"name": response.name})


def test_add_and_access_secret_version(secret_manager_client, project_id, unique_name):
    secret_id = f"test-secret-{unique_name}"
    parent = f"projects/{project_id}"
    payload = b"super-secret-value"

    secret = secret_manager_client.create_secret(
        request={
            "parent": parent,
            "secret_id": secret_id,
            "secret": {"replication": {"automatic": {}}},
        }
    )

    try:
        version = secret_manager_client.add_secret_version(
            request={"parent": secret.name, "payload": {"data": payload}}
        )
        assert version.name.endswith("/versions/1")

        accessed = secret_manager_client.access_secret_version(
            request={"name": f"{secret.name}/versions/latest"}
        )
        assert accessed.payload.data == payload
    finally:
        secret_manager_client.delete_secret(request={"name": secret.name})


def test_list_secret_versions(secret_manager_client, project_id, unique_name):
    secret_id = f"test-secret-{unique_name}"
    parent = f"projects/{project_id}"

    secret = secret_manager_client.create_secret(
        request={
            "parent": parent,
            "secret_id": secret_id,
            "secret": {"replication": {"automatic": {}}},
        }
    )

    try:
        secret_manager_client.add_secret_version(
            request={"parent": secret.name, "payload": {"data": b"v1"}}
        )
        secret_manager_client.add_secret_version(
            request={"parent": secret.name, "payload": {"data": b"v2"}}
        )

        versions = list(
            secret_manager_client.list_secret_versions(request={"parent": secret.name})
        )
        assert len(versions) >= 2
    finally:
        secret_manager_client.delete_secret(request={"name": secret.name})


def test_list_secrets(secret_manager_client, project_id, unique_name):
    secret_id = f"test-secret-{unique_name}"
    parent = f"projects/{project_id}"

    secret = secret_manager_client.create_secret(
        request={
            "parent": parent,
            "secret_id": secret_id,
            "secret": {"replication": {"automatic": {}}},
        }
    )

    try:
        secrets = [s.name for s in secret_manager_client.list_secrets(request={"parent": parent})]
        assert secret.name in secrets
    finally:
        secret_manager_client.delete_secret(request={"name": secret.name})


def test_delete_secret(secret_manager_client, project_id, unique_name):
    secret_id = f"test-secret-{unique_name}"
    parent = f"projects/{project_id}"

    secret = secret_manager_client.create_secret(
        request={
            "parent": parent,
            "secret_id": secret_id,
            "secret": {"replication": {"automatic": {}}},
        }
    )

    secret_manager_client.delete_secret(request={"name": secret.name})

    secrets = [s.name for s in secret_manager_client.list_secrets(request={"parent": parent})]
    assert secret.name not in secrets
