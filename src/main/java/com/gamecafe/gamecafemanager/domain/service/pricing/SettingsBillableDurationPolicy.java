package com.gamecafe.gamecafemanager.domain.service.pricing;

import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.service.SettingsProvider;
import java.time.Duration;
import java.util.Objects;

/**
 * Applies the current minimum and upward billing-interval settings.
 */
public final class SettingsBillableDurationPolicy implements BillableDurationPolicy {

    private final SettingsProvider settingsProvider;

    public SettingsBillableDurationPolicy(SettingsProvider settingsProvider) {
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
    }

    @Override
    public Duration calculateBillableDuration(Duration elapsedDuration) {
        Objects.requireNonNull(elapsedDuration, "elapsedDuration");
        ApplicationSettings settings = settingsProvider.getSettings();
        Duration billable = elapsedDuration;

        Integer minimumMinutes = settings.getMinimumSessionMinutes();
        if (minimumMinutes != null) {
            Duration minimum = Duration.ofMinutes(minimumMinutes.longValue());
            if (billable.compareTo(minimum) < 0) {
                billable = minimum;
            }
        }

        Integer roundingMinutes = settings.getBillingRoundingMinutes();
        if (roundingMinutes == null) {
            return billable;
        }
        long intervalSeconds = Math.multiplyExact(roundingMinutes.longValue(), 60L);
        long wholeSeconds = billable.getSeconds();
        if (billable.getNano() > 0) {
            wholeSeconds = Math.addExact(wholeSeconds, 1L);
        }
        long remainder = wholeSeconds % intervalSeconds;
        if (remainder != 0L) {
            wholeSeconds = Math.addExact(wholeSeconds, intervalSeconds - remainder);
        }
        return Duration.ofSeconds(wholeSeconds);
    }
}
