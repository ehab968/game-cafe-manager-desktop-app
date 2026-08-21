package com.gamecafe.gamecafemanager.domain.service.pricing;

import java.time.Duration;

/**
 * Extension point for converting actual elapsed time into billable time.
 */
@FunctionalInterface
public interface BillableDurationPolicy {

    Duration calculateBillableDuration(Duration elapsedDuration);

    static BillableDurationPolicy exactElapsedTime() {
        return elapsedDuration -> elapsedDuration;
    }
}
