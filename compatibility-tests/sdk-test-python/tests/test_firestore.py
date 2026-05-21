"""Firestore integration tests using google-cloud-firestore."""


def test_set_and_get_document(firestore_client, unique_name):
    col = firestore_client.collection(f"test-col-{unique_name}")
    doc_ref = col.document("alice")

    doc_ref.set({"name": "Alice", "age": 30, "active": True})

    doc = doc_ref.get()
    assert doc.exists
    assert doc.get("name") == "Alice"
    assert doc.get("age") == 30

    # Cleanup
    doc_ref.delete()


def test_query_documents(firestore_client, unique_name):
    col = firestore_client.collection(f"test-col-{unique_name}")

    col.document("user1").set({"name": "Alice", "score": 10})
    col.document("user2").set({"name": "Bob", "score": 20})
    col.document("user3").set({"name": "Charlie", "score": 10})

    try:
        results = list(col.where("score", "==", 10).stream())
        names = [r.get("name") for r in results]
        assert "Alice" in names
        assert "Charlie" in names
        assert "Bob" not in names
    finally:
        col.document("user1").delete()
        col.document("user2").delete()
        col.document("user3").delete()


def test_update_document(firestore_client, unique_name):
    col = firestore_client.collection(f"test-col-{unique_name}")
    doc_ref = col.document("update-test")

    doc_ref.set({"name": "Alice", "age": 30})
    doc_ref.update({"age": 31})

    doc = doc_ref.get()
    assert doc.get("age") == 31
    assert doc.get("name") == "Alice"

    # Cleanup
    doc_ref.delete()


def test_delete_document(firestore_client, unique_name):
    col = firestore_client.collection(f"test-col-{unique_name}")
    doc_ref = col.document("to-delete")

    doc_ref.set({"name": "Delete Me"})
    doc_ref.delete()

    doc = doc_ref.get()
    assert not doc.exists


def test_batch_write(firestore_client, unique_name):
    col = firestore_client.collection(f"test-col-{unique_name}")

    batch = firestore_client.batch()
    ref1 = col.document("batch1")
    ref2 = col.document("batch2")
    batch.set(ref1, {"name": "Batch1"})
    batch.set(ref2, {"name": "Batch2"})
    batch.commit()

    try:
        assert ref1.get().get("name") == "Batch1"
        assert ref2.get().get("name") == "Batch2"
    finally:
        ref1.delete()
        ref2.delete()
