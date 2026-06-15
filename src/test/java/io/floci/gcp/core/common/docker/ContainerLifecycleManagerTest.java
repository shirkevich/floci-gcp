package io.floci.gcp.core.common.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Closeable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContainerLifecycleManagerTest {

    @Mock
    DockerClient dockerClient;

    @Mock
    ContainerDetector containerDetector;

    @Mock
    PortAllocator portAllocator;

    @Mock
    ImageCacheService imageCacheService;

    @Mock
    RemoveContainerCmd removeContainerCmd;

    @Mock
    Closeable logStream;

    @Test
    void forceRemoveClosesLogsAndRemovesContainerWithoutGracefulStop() throws Exception {
        when(dockerClient.removeContainerCmd("container-1")).thenReturn(removeContainerCmd);
        when(removeContainerCmd.withForce(true)).thenReturn(removeContainerCmd);

        manager().forceRemove("container-1", logStream);

        InOrder inOrder = inOrder(logStream, dockerClient, removeContainerCmd);
        inOrder.verify(logStream).close();
        inOrder.verify(dockerClient).removeContainerCmd("container-1");
        inOrder.verify(removeContainerCmd).withForce(true);
        inOrder.verify(removeContainerCmd).exec();
        verify(dockerClient, never()).stopContainerCmd(anyString());
    }

    @Test
    void forceRemoveToleratesDockerRemovalFailures() {
        when(dockerClient.removeContainerCmd("container-1")).thenReturn(removeContainerCmd);
        when(removeContainerCmd.withForce(true)).thenReturn(removeContainerCmd);
        when(removeContainerCmd.exec()).thenThrow(new RuntimeException("docker unavailable"));

        assertDoesNotThrow(() -> manager().forceRemove("container-1", null));
    }

    private ContainerLifecycleManager manager() {
        return new ContainerLifecycleManager(dockerClient, containerDetector, portAllocator, imageCacheService);
    }
}
