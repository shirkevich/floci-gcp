package io.floci.gcp.services.pubsub;

import com.google.protobuf.Empty;
import com.google.pubsub.v1.*;
import io.floci.gcp.core.common.GcpGrpcController;
import io.floci.gcp.services.pubsub.model.StoredSubscription;
import io.grpc.stub.StreamObserver;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class PubSubSubscriberController extends SubscriberGrpc.SubscriberImplBase {

    private static final Logger LOG = Logger.getLogger(PubSubSubscriberController.class);

    private final PubSubService service;

    PubSubSubscriberController(PubSubService service) {
        this.service = service;
    }

    @Override
    public void createSubscription(Subscription request, StreamObserver<Subscription> responseObserver) {
        LOG.infof("createSubscription name=%s topic=%s", request.getName(), request.getTopic());
        try {
            StoredSubscription stored = service.createSubscription(
                    request.getName(), request.getTopic(), request.getAckDeadlineSeconds());
            responseObserver.onNext(buildSubscription(stored));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.warnf("createSubscription failed: %s", e.getMessage());
            GcpGrpcController.grpcError(responseObserver, e);
        }
    }

    @Override
    public void getSubscription(GetSubscriptionRequest request, StreamObserver<Subscription> responseObserver) {
        LOG.debugf("getSubscription name=%s", request.getSubscription());
        try {
            StoredSubscription stored = service.getSubscription(request.getSubscription());
            responseObserver.onNext(buildSubscription(stored));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.warnf("getSubscription failed: %s", e.getMessage());
            GcpGrpcController.grpcError(responseObserver, e);
        }
    }

    @Override
    public void listSubscriptions(ListSubscriptionsRequest request,
            StreamObserver<ListSubscriptionsResponse> responseObserver) {
        LOG.debugf("listSubscriptions project=%s", request.getProject());
        try {
            String project = extractProjectId(request.getProject());
            List<StoredSubscription> subs = service.listSubscriptions(project);
            ListSubscriptionsResponse.Builder response = ListSubscriptionsResponse.newBuilder();
            for (StoredSubscription s : subs) {
                response.addSubscriptions(buildSubscription(s));
            }
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.warnf("listSubscriptions failed: %s", e.getMessage());
            GcpGrpcController.grpcError(responseObserver, e);
        }
    }

    @Override
    public void deleteSubscription(DeleteSubscriptionRequest request, StreamObserver<Empty> responseObserver) {
        LOG.infof("deleteSubscription name=%s", request.getSubscription());
        try {
            service.deleteSubscription(request.getSubscription());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.warnf("deleteSubscription failed: %s", e.getMessage());
            GcpGrpcController.grpcError(responseObserver, e);
        }
    }

    @Override
    public void pull(PullRequest request, StreamObserver<PullResponse> responseObserver) {
        LOG.debugf("pull subscription=%s maxMessages=%d", request.getSubscription(), request.getMaxMessages());
        try {
            int max = request.getMaxMessages() > 0 ? request.getMaxMessages() : 1;
            List<ReceivedMessage> messages = service.pull(request.getSubscription(), max);
            LOG.debugf("pull subscription=%s returned=%d", request.getSubscription(), messages.size());
            responseObserver.onNext(PullResponse.newBuilder().addAllReceivedMessages(messages).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.warnf("pull failed: %s", e.getMessage());
            GcpGrpcController.grpcError(responseObserver, e);
        }
    }

    @Override
    public void acknowledge(AcknowledgeRequest request, StreamObserver<Empty> responseObserver) {
        LOG.debugf("acknowledge subscription=%s ackIds=%d", request.getSubscription(), request.getAckIdsCount());
        try {
            service.acknowledge(request.getSubscription(), request.getAckIdsList());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.warnf("acknowledge failed: %s", e.getMessage());
            GcpGrpcController.grpcError(responseObserver, e);
        }
    }

    @Override
    public void modifyAckDeadline(ModifyAckDeadlineRequest request, StreamObserver<Empty> responseObserver) {
        LOG.debugf("modifyAckDeadline subscription=%s ackIds=%d deadline=%d",
                request.getSubscription(), request.getAckIdsCount(), request.getAckDeadlineSeconds());
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<StreamingPullRequest> streamingPull(
            StreamObserver<StreamingPullResponse> responseObserver) {
        AtomicReference<String> subscriptionRef = new AtomicReference<>();

        return new StreamObserver<>() {
            @Override
            public void onNext(StreamingPullRequest request) {
                try {
                    if (!request.getSubscription().isEmpty()) {
                        String sub = request.getSubscription();
                        subscriptionRef.set(sub);
                        LOG.infof("streamingPull opened subscription=%s", sub);
                    }
                    String sub = subscriptionRef.get();
                    if (sub == null) {
                        return;
                    }
                    if (!request.getAckIdsList().isEmpty()) {
                        LOG.debugf("streamingPull ack subscription=%s ackIds=%d", sub, request.getAckIdsCount());
                        service.acknowledge(sub, request.getAckIdsList());
                    }
                    List<ReceivedMessage> messages = service.pull(sub, 1000);
                    if (!messages.isEmpty()) {
                        LOG.debugf("streamingPull deliver subscription=%s messages=%d", sub, messages.size());
                        responseObserver.onNext(
                                StreamingPullResponse.newBuilder()
                                        .addAllReceivedMessages(messages)
                                        .build());
                    }
                } catch (Exception e) {
                    LOG.warnf("streamingPull error: %s", e.getMessage());
                    responseObserver.onError(GcpGrpcController.grpcException(e));
                }
            }

            @Override
            public void onError(Throwable t) {
                LOG.debugf("streamingPull stream closed by client: %s", t.getMessage());
            }

            @Override
            public void onCompleted() {
                LOG.debugf("streamingPull stream completed subscription=%s", subscriptionRef.get());
                responseObserver.onCompleted();
            }
        };
    }

    private static Subscription buildSubscription(StoredSubscription stored) {
        return Subscription.newBuilder()
                .setName(stored.getName())
                .setTopic(stored.getTopic())
                .setAckDeadlineSeconds(stored.getAckDeadlineSeconds())
                .build();
    }

    private static String extractProjectId(String parent) {
        if (parent.startsWith("projects/")) {
            return parent.substring("projects/".length());
        }
        return parent;
    }
}
