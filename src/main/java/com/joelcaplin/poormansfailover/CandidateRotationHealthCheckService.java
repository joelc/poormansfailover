package com.joelcaplin.poormansfailover;

import lombok.extern.slf4j.Slf4j;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Predicate;

@Slf4j
public class CandidateRotationHealthCheckService<T> implements AutoCloseable {
    private CandidateRotation<T> rotation;
    private Predicate<T> candidateIsAlivePredicate;

    private Timer timer;

    public CandidateRotationHealthCheckService(CandidateRotation<T> rotation,
                                               Predicate<T> candidateIsAlivePredicate,
                                               long delay,
                                               long period) {
        timer = new Timer(true);

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                checkOutOfRotationCandidates();
            }
        };

        timer.schedule(timerTask, delay, period);
    }

    public void checkOutOfRotationCandidates() {
        log.debug("Checking out of rotation candidates for health...");

        rotation.getOutOfRotationCandidates()
                .stream()
                .filter(candidate -> candidateIsAlivePredicate.test(candidate.getReference()))
                .peek(candidate -> log.info("Candidate {} is alive - putting back in rotation", candidate))
                .forEach(rotation::tryPutBackInRotation);
    }

    @Override
    public void close() {
        log.info("closing");

        timer.cancel();
    }
}
