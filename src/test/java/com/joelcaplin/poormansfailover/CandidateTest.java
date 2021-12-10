package com.joelcaplin.poormansfailover;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CandidateTest {
    @Test
    public void shouldBeComparableByPriority() {
        // given
        Candidate primary = Candidate.of("PRIMARY", Priority.PRIMARY);
        Candidate secondary = Candidate.of("SECONDARY", Priority.SECONDARY);
        Candidate tertiary = Candidate.of("TERTIARY", Priority.TERTIARY);

        // then
        assertThat(primary).isGreaterThan(secondary);
        assertThat(secondary).isGreaterThan(tertiary);
    }
}
