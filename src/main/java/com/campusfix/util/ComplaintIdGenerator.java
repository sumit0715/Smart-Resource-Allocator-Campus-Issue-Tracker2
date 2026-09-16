package com.campusfix.util;

import java.time.Year;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Produces IDs like CMP-2026-00124. AtomicInteger gives thread-safe increments,
 * which matters once several students can submit complaints at the same time
 * (see ComplaintSubmissionTask).
 */
public final class ComplaintIdGenerator {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private ComplaintIdGenerator() {
    }

    public static String nextId() {
        int sequence = COUNTER.incrementAndGet();
        return String.format("CMP-%d-%05d", Year.now().getValue(), sequence);
    }

    /** Lets numbering resume from where the database left off after a restart. */
    public static void seedFrom(int highestKnownSequence) {
        COUNTER.updateAndGet(current -> Math.max(current, highestKnownSequence));
    }
}
