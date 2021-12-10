package com.joelcaplin.poormansfailover;

public enum Priority implements Comparable<Priority> {
    // order is explicitly reversed so that (PRIMARY.comparedTo(SECONDARY) is always > 0)
    TERTIARY,
    SECONDARY,
    PRIMARY;
}
