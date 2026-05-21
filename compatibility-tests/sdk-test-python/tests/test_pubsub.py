"""Pub/Sub integration tests using google-cloud-pubsub."""

import time
import pytest


def test_create_topic(pubsub_publisher, project_id, unique_name):
    topic_path = pubsub_publisher.topic_path(project_id, f"test-topic-{unique_name}")
    topic = pubsub_publisher.create_topic(request={"name": topic_path})
    assert topic.name == topic_path

    # Cleanup
    pubsub_publisher.delete_topic(request={"topic": topic_path})


def test_create_subscription(pubsub_publisher, pubsub_subscriber, project_id, unique_name):
    topic_id = f"test-topic-{unique_name}"
    sub_id = f"test-sub-{unique_name}"
    topic_path = pubsub_publisher.topic_path(project_id, topic_id)
    sub_path = pubsub_subscriber.subscription_path(project_id, sub_id)

    pubsub_publisher.create_topic(request={"name": topic_path})
    sub = pubsub_subscriber.create_subscription(request={"name": sub_path, "topic": topic_path})

    assert sub.name == sub_path
    assert sub.topic == topic_path

    # Cleanup
    pubsub_subscriber.delete_subscription(request={"subscription": sub_path})
    pubsub_publisher.delete_topic(request={"topic": topic_path})


def test_publish_and_pull_messages(pubsub_publisher, pubsub_subscriber, project_id, unique_name):
    topic_id = f"test-topic-{unique_name}"
    sub_id = f"test-sub-{unique_name}"
    topic_path = pubsub_publisher.topic_path(project_id, topic_id)
    sub_path = pubsub_subscriber.subscription_path(project_id, sub_id)

    pubsub_publisher.create_topic(request={"name": topic_path})
    pubsub_subscriber.create_subscription(request={"name": sub_path, "topic": topic_path})

    try:
        # Publish messages
        future1 = pubsub_publisher.publish(topic_path, b"Hello, GCP Pub/Sub from Python!")
        future2 = pubsub_publisher.publish(topic_path, b"Second message", source="python-test")
        id1 = future1.result(timeout=10)
        id2 = future2.result(timeout=10)

        assert id1
        assert id2
        assert id1 != id2

        # Pull messages
        time.sleep(0.2)
        response = pubsub_subscriber.pull(
            request={"subscription": sub_path, "max_messages": 10}
        )

        assert len(response.received_messages) >= 2

        bodies = [m.message.data.decode() for m in response.received_messages]
        assert "Hello, GCP Pub/Sub from Python!" in bodies
        assert "Second message" in bodies

        # Acknowledge
        ack_ids = [m.ack_id for m in response.received_messages]
        pubsub_subscriber.acknowledge(request={"subscription": sub_path, "ack_ids": ack_ids})
    finally:
        pubsub_subscriber.delete_subscription(request={"subscription": sub_path})
        pubsub_publisher.delete_topic(request={"topic": topic_path})


def test_list_topics(pubsub_publisher, project_id, unique_name):
    topic_id = f"test-topic-{unique_name}"
    topic_path = pubsub_publisher.topic_path(project_id, topic_id)
    pubsub_publisher.create_topic(request={"name": topic_path})

    try:
        topics = [t.name for t in pubsub_publisher.list_topics(request={"project": f"projects/{project_id}"})]
        assert topic_path in topics
    finally:
        pubsub_publisher.delete_topic(request={"topic": topic_path})
