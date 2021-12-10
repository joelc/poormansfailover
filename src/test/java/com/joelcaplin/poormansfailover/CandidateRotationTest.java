package com.joelcaplin.poormansfailover;

import com.sun.tools.javac.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Comparator;
import java.util.PriorityQueue;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@ExtendWith(MockitoExtension.class)
public class CandidateRotationTest {
    private static final Candidate<String> TEST_PRIMARY_CANDIDATE = Candidate.of("PRIMARY", Priority.PRIMARY);
    private static final Candidate<String> TEST_SECONDARY_CANDIDATE = Candidate.of("SECONDARY", Priority.SECONDARY);

    private CandidateRotation<String> sut;

    @BeforeEach
    public void init() {
        PriorityQueue<Candidate<String>> pq = new PriorityQueue<>(Comparator.reverseOrder());
        pq.addAll(asList(TEST_PRIMARY_CANDIDATE, TEST_SECONDARY_CANDIDATE));

        sut = new CandidateRotation<>(pq);
    }

    @Test
    public void shouldGetPrimaryCandidate() {
        // when
        Candidate<String> peeked = sut.peek();

        // then
        assertThat(peeked).isEqualTo(TEST_PRIMARY_CANDIDATE);
    }

    @Test
    public void shouldGetSecondaryWhenPrimaryTakenOutOfRotation() {
        // given
        sut.tryRemoveFromRotation(TEST_PRIMARY_CANDIDATE);

        // when
        Candidate<String> peeked = sut.peek();

        // then
        assertThat(peeked).isEqualTo(TEST_SECONDARY_CANDIDATE);
    }

    @Test
    public void shouldPutCandidateBackIntoRotation() {
        // given
        sut.tryRemoveFromRotation(TEST_PRIMARY_CANDIDATE);
        sut.tryPutBackInRotation(TEST_PRIMARY_CANDIDATE);

        // when
        Candidate<String> peeked = sut.peek();

        // then
        assertThat(peeked).isEqualTo(TEST_PRIMARY_CANDIDATE);
    }

    @Test
    public void shouldNotPutAlreadyInRotationCandidateBackIntoRotationAgain() {
        // given
        sut.tryPutBackInRotation(TEST_PRIMARY_CANDIDATE);
        sut.tryRemoveFromRotation(TEST_PRIMARY_CANDIDATE);

        // when
        Candidate<String> peeked = sut.peek();

        // then
        assertThat(peeked).isEqualTo(TEST_SECONDARY_CANDIDATE);
    }

    @Test
    public void shouldReturnUnmodifiableOutOfRotationSet() {
        // when
        ThrowingCallable tc = () -> {
            sut.getOutOfRotationCandidates()
               .clear();
        };

        // then
        assertThatThrownBy(tc).isInstanceOf(UnsupportedOperationException.class);
    }

}
