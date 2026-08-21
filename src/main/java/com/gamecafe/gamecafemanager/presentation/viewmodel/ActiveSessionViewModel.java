package com.gamecafe.gamecafemanager.presentation.viewmodel;

import com.gamecafe.gamecafemanager.domain.model.Session;
import java.math.BigDecimal;
import java.time.Duration;
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
    private final ReadOnlyStringWrapper elapsedText = new ReadOnlyStringWrapper("00:00:00");

    public ActiveSessionViewModel(Session session) {
        Objects.requireNonNull(session, "session");
        if (session.getId() == null) {
            throw new IllegalArgumentException("Persisted session id is required");
        }
        this.sessionId = session.getId();
        this.stationId = session.getStationId();
        this.stationName = session.getStationNameSnapshot();
        this.startTime = session.getStartTime();
        this.hourlyRateSnapshot = session.getHourlyRateSnapshot();
    }

    public void refresh(Instant currentTime) {
        Objects.requireNonNull(currentTime, "currentTime");
        Duration elapsed = currentTime.isBefore(startTime)
                ? Duration.ZERO
                : Duration.between(startTime, currentTime);
        elapsedText.set(formatDuration(elapsed));
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

    private String formatDuration(Duration duration) {
        long totalSeconds = duration.getSeconds();
        long hours = totalSeconds / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
