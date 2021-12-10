package com.joelcaplin.poormansfailover;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

@Slf4j
public class CandidateRotation<T> {
    private final PriorityBlockingQueue<Candidate<T>> candidateQueue;
    private final Set<Candidate<T>> outOfRotationSet;

    public CandidateRotation(Collection<Candidate<T>> candidates) {
        candidateQueue = new PriorityBlockingQueue<Candidate<T>>(3, Comparator.reverseOrder());
        candidateQueue.addAll(candidates);

        outOfRotationSet = ConcurrentHashMap.newKeySet();
    }

    public Candidate<T> peek() {
        return candidateQueue.peek();
    }

    public void tryRemoveFromRotation(Candidate<T> candidate) {
        if (candidateQueue.remove(candidate)) {
            log.debug("Remove candidate {} from rotation", candidate);
            outOfRotationSet.add(candidate);
        }
    }

    public void tryPutBackInRotation(Candidate<T> candidate) {
        if (outOfRotationSet.remove(candidate)) {
            log.debug("Putting candidate {} back into rotation", candidate);
            candidateQueue.add(candidate);
        }
    }


    public Collection<Candidate<T>> getOutOfRotationCandidates() {
        return Collections.unmodifiableCollection(outOfRotationSet);
    }
}
