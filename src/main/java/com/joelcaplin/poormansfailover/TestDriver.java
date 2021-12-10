package com.joelcaplin.poormansfailover;

import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.function.Predicate;

import static java.util.Arrays.asList;

public class TestDriver {
    public static void main(String[] args) {
        // create your us-east-1 and us-west-1 clients here
        S3AsyncClient s3_us_east_1 = null;
        S3AsyncClient s3_us_west_1 = null;

        // make candidates of them, with us-east-1 being highest priority
        Candidate<S3AsyncClient> primaryS3Candidate =
                Candidate.of(s3_us_east_1, Priority.PRIMARY);

        Candidate<S3AsyncClient> secondaryS3Candidate =
                Candidate.of(s3_us_west_1, Priority.SECONDARY);

        // create a rotation to put them in
        CandidateRotation<S3AsyncClient> rotation = new CandidateRotation<>(asList(primaryS3Candidate, secondaryS3Candidate));

        // create a predicate that is used to check whether a previously unhealthy S3
        // client is now healthy again
        Predicate<S3AsyncClient> clientIsHealthyPredicate = null;

        // create a service to monitor their health
        CandidateRotationHealthCheckService<S3AsyncClient> healthCheckService =
                new CandidateRotationHealthCheckService<>(rotation,
                                                          clientIsHealthyPredicate,
                                                          1000,
                                                          1000);

        // create soemthing that evaluates throwables which originate from client calls to determine: is this throwable
        // indicative that this S3 region is down?
        Predicate<Throwable> serviceFailureThrowablePredicate = null;

        // create something that'll fulfill requests for methods that return CompletableFuture
        InvocationHandler invocationHandler = new CandidateRotationCompletableFutureInvocationHandler<>(rotation, serviceFailureThrowablePredicate);

        // create a JDK Proxy for S3AsyncClient
        S3AsyncClient multiClient =
                (S3AsyncClient) Proxy.newProxyInstance(S3AsyncClient.class.getClassLoader(),
                                                       new Class<?>[]{S3AsyncClient.class},
                                                       invocationHandler);

        // do shit
        multiClient.listObjects(c -> c.bucket("some-bucket"));
    }
}
