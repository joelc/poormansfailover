package com.joelcaplin.poormansfailover;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class PriorityTest {
    @Test
    public void shouldComparePrimaryToBeGreaterThanSecondary() {
        // given
        Priority primary = Priority.PRIMARY;
        Priority secondary = Priority.SECONDARY;

        // then
        assertThat(primary).isGreaterThan(secondary);
    }

    @Test
    public void shouldCompareSecondaryToBeGreaterThanTertiary() {
        // given
        Priority secondary = Priority.SECONDARY;
        Priority tertiary = Priority.TERTIARY;

        // then
        assertThat(secondary).isGreaterThan(tertiary);
    }
}
