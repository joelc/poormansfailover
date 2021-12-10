package com.joelcaplin.poormansfailover;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@Slf4j
public class CandidateRotationCompletableFutureInvocationHandler<T> implements InvocationHandler {
    private final CandidateRotation<T> candidateRotation;
    private final Predicate<Throwable> serviceFailureThrowablePredicate;

    public CandidateRotationCompletableFutureInvocationHandler(CandidateRotation<T> candidateRotation,
                                                               Predicate<Throwable> serviceFailureThrowablePredicate) {
        this.candidateRotation = candidateRotation;
        this.serviceFailureThrowablePredicate = serviceFailureThrowablePredicate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return doInvoke(proxy, method, args);
    }

    private <R> CompletableFuture<R> doInvoke(Object proxy,
                                              Method method,
                                              Object[] args) {
        final Candidate<T> candidate = candidateRotation.peek();

        if (candidate == null) {
            return exceptionallyCompleted(new IllegalStateException("No candidates in rotation"));
        }

        CompletableFuture<R> cf = null;

        try {
            cf = (CompletableFuture) candidate.invokeWithReference(method, args);
        } catch (Throwable t) {
            return exceptionallyCompleted(t);
        }

        return cf.handle((resp, ex) -> doHandle(proxy, method, args, candidate, resp, ex))
                 .thenCompose(x -> x);
    }

    private <R> CompletableFuture<R> doHandle(Object proxy,
                                              Method method,
                                              Object[] args,
                                              Candidate<T> candidate,
                                              R response,
                                              Throwable t) {
        if (t == null) {
            log.debug("Candidate {} invocation of {} succeeded - returning response", candidate, method);
            return CompletableFuture.<R>completedFuture(response);
        }

        if (!serviceFailureThrowablePredicate.test(t)) {
            log.debug("Candidate {} invocation of {} threw an exception which is NOT indicative of service failure - rethrowing", candidate, method, t);
            return this.<R>exceptionallyCompleted(t);
        }

        log.warn("Candidate {} invocation of {} threw an exception which IS indicative of service failure - removing from rotation and moving on", candidate, method, t);

        candidateRotation.tryRemoveFromRotation(candidate);

        return this.<R>doInvoke(proxy, method, args);
    }

    private <R> CompletableFuture<R> exceptionallyCompleted(Throwable t) {
        CompletableFuture<R> cf = new CompletableFuture();
        cf.completeExceptionally(t);

        return cf;
    }
}
