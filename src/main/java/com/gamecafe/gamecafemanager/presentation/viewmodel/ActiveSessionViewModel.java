package com.gamecafe.gamecafemanager.presentation.viewmodel;

import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.domain.service.pricing.PricingResult;
import com.gamecafe.gamecafemanager.presentation.format.ApplicationDisplayService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

/**
 * Presentation state for an active session timer. Elapsed time is always
 * derived from the persisted start time and the supplied current time.
 */
public final class ActiveSessionViewModel {

    private final long sessionId;
    private final long stationId;
    private final String stationName;
    private final Instant startTime;
    private final BigDecimal hourlyRateSnapshot;
    private final ApplicationDisplayService displayService;
    private final ReadOnlyStringWrapper elapsedText = new ReadOnlyStringWrapper("00:00:00");
    private final ReadOnlyStringWrapper currentGamingCost = new ReadOnlyStringWrapper();
    private long displayedElapsedSeconds = Long.MIN_VALUE;
    private BigDecimal displayedGamingPrice;

    public ActiveSessionViewModel(
            Session session,
            ApplicationDisplayService displayService) {
        Objects.requireNonNull(session, "session");
        this.displayService = Objects.requireNonNull(displayService, "displayService");
        currentGamingCost.set(displayService.formatMoney(BigDecimal.ZERO.setScale(2)));
        if (session.getId() == null) {
            throw new IllegalArgumentException("Persisted session id is required");
        }
        this.sessionId = session.getId();
        this.stationId = session.getStationId();
        this.stationName = session.getStationNameSnapshot();
        this.startTime = session.getStartTime();
        this.hourlyRateSnapshot = session.getHourlyRateSnapshot();
    }

    public void refresh(Instant currentTime, PricingService pricingService) {
        Objects.requireNonNull(currentTime, "currentTime");
        Objects.requireNonNull(pricingService, "pricingService");
        Instant effectiveTime = currentTime.isBefore(startTime) ? startTime : currentTime;
        PricingResult result = pricingService.calculate(
                startTime,
                effectiveTime,
                hourlyRateSnapshot);
        long elapsedSeconds = result.getElapsedDuration().getSeconds();
        if (elapsedSeconds != displayedElapsedSeconds) {
            displayedElapsedSeconds = elapsedSeconds;
            elapsedText.set(formatDuration(elapsedSeconds));
        }
        BigDecimal gamingPrice = result.getGamingPrice();
        if (displayedGamingPrice == null
                || gamingPrice.compareTo(displayedGamingPrice) != 0) {
            displayedGamingPrice = gamingPrice;
            currentGamingCost.set(displayService.formatMoney(gamingPrice));
        }
    }

    public long getSessionId() {
        return sessionId;
    }

    public long getStationId() {
        return stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public BigDecimal getHourlyRateSnapshot() {
        return hourlyRateSnapshot;
    }

    public String getElapsedText() {
        return elapsedText.get();
    }

    public ReadOnlyStringProperty elapsedTextProperty() {
        return elapsedText.getReadOnlyProperty();
    }

    public String getCurrentGamingCost() {
        return currentGamingCost.get();
    }

    public ReadOnlyStringProperty currentGamingCostProperty() {
        return currentGamingCost.getReadOnlyProperty();
    }

    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;
        StringBuilder formatted = new StringBuilder(8);
        appendTwoDigits(formatted, hours);
        formatted.append(':');
        appendTwoDigits(formatted, minutes);
        formatted.append(':');
        appendTwoDigits(formatted, seconds);
        return formatted.toString();
    }

    private void appendTwoDigits(StringBuilder target, long value) {
        if (value < 10L) {
            target.append('0');
        }
        target.append(value);
    }
}
