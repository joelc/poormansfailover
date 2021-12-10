package com.joelcaplin.poormansfailover;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CandidateRotationCompletableFutureInvocationHandlerTest {
    @Mock
    private CandidateRotation<S3AsyncClient> mockRotation;

    @Mock
    private Candidate<S3AsyncClient> mockPrimaryClient;

    @Mock
    private Candidate<S3AsyncClient> mockSecondaryClient;

    @Mock
    private ListObjectsResponse mockListObjectsResponse;

    @Mock
    private Predicate<Throwable> mockServiceFailureExceptionPredicate;

    @InjectMocks
    private CandidateRotationCompletableFutureInvocationHandler sut;

    private static Method listObjectsMethod;

    @BeforeAll
    public static void staticInit() throws NoSuchMethodException {
        listObjectsMethod = S3AsyncClient.class.getMethod("listObjects", new Class[]{ListObjectsRequest.class});
    }

    @Test
    public void shouldDelegateToPrimaryClient() throws Throwable {
        // given
        when(mockRotation.peek()).thenReturn(mockPrimaryClient);
        when(mockPrimaryClient.invokeWithReference(listObjectsMethod, new Object[0])).thenReturn(CompletableFuture.completedFuture(mockListObjectsResponse));

        // when
        CompletableFuture<ListObjectsResponse> cf = (CompletableFuture<ListObjectsResponse>) sut.invoke(null, listObjectsMethod, new Object[0]);

        // then
        assertThat(cf.join()).isEqualTo(mockListObjectsResponse);
    }

    @Test
    public void shouldRemovePrimaryClientOnException() throws Throwable {
        // given
        when(mockRotation.peek()).thenReturn(mockPrimaryClient, mockSecondaryClient);
        when(mockPrimaryClient.invokeWithReference(listObjectsMethod, new Object[0])).thenReturn(CompletableFuture.runAsync(() -> {
            throw new IllegalStateException();
        }));
        when(mockSecondaryClient.invokeWithReference(listObjectsMethod, new Object[0])).thenReturn(CompletableFuture.completedFuture(mockListObjectsResponse));
        when(mockServiceFailureExceptionPredicate.test(any(Throwable.class)))
                .thenReturn(true);

        // when
        CompletableFuture<ListObjectsResponse> cf = (CompletableFuture<ListObjectsResponse>) sut.invoke(null, listObjectsMethod, new Object[0]);

        // then
        cf.join();
        verify(mockRotation, times(1)).tryRemoveFromRotation(mockPrimaryClient);
    }

    @Test
    public void shouldWrapCompletableFutureAroundInvocationException() throws Throwable {
        // given
        Exception e = new RuntimeException();
        when(mockRotation.peek()).thenReturn(mockPrimaryClient);
        when(mockPrimaryClient.invokeWithReference(listObjectsMethod, new Object[0])).thenThrow(e);

        // when
        CompletableFuture cf = (CompletableFuture) sut.invoke(null, listObjectsMethod, new Object[0]);

        // then
        assertThatThrownBy(() -> cf.join()).isInstanceOf(CompletionException.class)
                                           .hasRootCause(e);
    }

    @Test
    public void shouldUseNextEnqueuedCandidateOnException() throws Throwable {
        // given
        when(mockRotation.peek()).thenReturn(mockPrimaryClient, mockSecondaryClient);
        when(mockPrimaryClient.invokeWithReference(listObjectsMethod, new Object[0])).thenReturn(CompletableFuture.runAsync(() -> {
            throw new IllegalStateException("TEST");
        }));
        when(mockSecondaryClient.invokeWithReference(listObjectsMethod, new Object[0])).thenReturn(CompletableFuture.completedFuture(mockListObjectsResponse));
        when(mockServiceFailureExceptionPredicate.test(any(Throwable.class)))
                .thenReturn(true);

        // when
        CompletableFuture<ListObjectsResponse> cf = (CompletableFuture<ListObjectsResponse>) sut.invoke(null, listObjectsMethod, new Object[0]);

        // then
        assertThat(cf.join()).isEqualTo(mockListObjectsResponse);
    }

    @Test
    public void shouldRethrowIfExceptionNotIndicativeOfServiceFailure() throws Throwable {
        // given
        IllegalStateException expected = new IllegalStateException("TEST");
        when(mockRotation.peek()).thenReturn(mockPrimaryClient, null);
        when(mockPrimaryClient.invokeWithReference(listObjectsMethod, new Object[0])).thenReturn(CompletableFuture.runAsync(() -> {
            throw expected;
        }));

        // when
        CompletableFuture<ListObjectsResponse> cf = (CompletableFuture<ListObjectsResponse>) sut.invoke(null, listObjectsMethod, new Object[0]);

        // then
        assertThatThrownBy(() -> cf.join()).isInstanceOf(CompletionException.class)
                                           .hasRootCause(expected);

    }

    @Test
    public void shouldThrowIllegalStateExceptionIfNoCandidatesAvailable() throws Throwable {
        // given
        when(mockRotation.peek()).thenReturn(null);

        // when
        CompletableFuture cf = (CompletableFuture) sut.invoke(null, listObjectsMethod, new Object[0]);

        // then
        assertThatThrownBy(() -> cf.join()).isInstanceOf(CompletionException.class)
                                           .hasRootCauseInstanceOf(IllegalStateException.class);
    }
}
