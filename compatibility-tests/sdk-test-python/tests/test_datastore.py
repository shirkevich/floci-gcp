"""Datastore integration tests using google-cloud-datastore."""

from google.cloud import datastore


def test_put_and_get_entity(datastore_client, unique_name):
    key = datastore_client.key("Task", f"task-{unique_name}")
    entity = datastore.Entity(key=key)
    entity.update({"description": "Buy groceries", "done": False, "priority": 4})

    datastore_client.put(entity)

    fetched = datastore_client.get(key)
    assert fetched is not None
    assert fetched["description"] == "Buy groceries"
    assert fetched["done"] is False
    assert fetched["priority"] == 4

    # Cleanup
    datastore_client.delete(key)


def test_query_entities(datastore_client, unique_name):
    key1 = datastore_client.key("Task", f"task1-{unique_name}")
    key2 = datastore_client.key("Task", f"task2-{unique_name}")
    e1 = datastore.Entity(key=key1)
    e1.update({"description": "Task A", "done": False})
    e2 = datastore.Entity(key=key2)
    e2.update({"description": "Task B", "done": True})

    datastore_client.put_multi([e1, e2])

    try:
        query = datastore_client.query(kind="Task")
        query.add_filter("done", "=", False)
        results = list(query.fetch())
        descriptions = [r["description"] for r in results]
        assert "Task A" in descriptions
    finally:
        datastore_client.delete_multi([key1, key2])


def test_update_entity(datastore_client, unique_name):
    key = datastore_client.key("Task", f"update-task-{unique_name}")
    entity = datastore.Entity(key=key)
    entity.update({"description": "Original", "done": False})
    datastore_client.put(entity)

    try:
        fetched = datastore_client.get(key)
        fetched["done"] = True
        datastore_client.put(fetched)

        updated = datastore_client.get(key)
        assert updated["done"] is True
    finally:
        datastore_client.delete(key)


def test_delete_entity(datastore_client, unique_name):
    key = datastore_client.key("Task", f"delete-task-{unique_name}")
    entity = datastore.Entity(key=key)
    entity.update({"description": "Delete Me"})
    datastore_client.put(entity)

    datastore_client.delete(key)

    fetched = datastore_client.get(key)
    assert fetched is None


def test_allocate_id(datastore_client):
    key = datastore_client.key("Task")
    [allocated_key] = datastore_client.allocate_ids(key, 1)
    assert allocated_key.id is not None
    assert allocated_key.id > 0
