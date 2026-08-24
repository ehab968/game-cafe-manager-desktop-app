package com.gamecafe.gamecafemanager.domain.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Output-neutral invoice document. Presentation, PDF, and printing adapters can
 * render this model without recalculating checkout values.
 */
public final class Invoice {

    private final String cafeName;
    private final String currencyDisplay;
    private final String footer;
    private final String invoiceNumber;
    private final long sessionId;
    private final String stationName;
    private final StationType stationType;
    private final SessionMode mode;
    private final BigDecimal hourlyRateSnapshot;
    private final Instant startTime;
    private final Instant endTime;
    private final Duration duration;
    private final BigDecimal gamingAmount;
    private final List<InvoiceItem> purchasedProducts;
    private final BigDecimal productsTotal;
    private final BigDecimal total;

    public Invoice(
            String cafeName,
            String currencyDisplay,
            String footer,
            String invoiceNumber,
            long sessionId,
            String stationName,
            StationType stationType,
            SessionMode mode,
            BigDecimal hourlyRateSnapshot,
            Instant startTime,
            Instant endTime,
            Duration duration,
            BigDecimal gamingAmount,
            List<InvoiceItem> purchasedProducts,
            BigDecimal productsTotal,
            BigDecimal total) {
        this.cafeName = Objects.requireNonNull(cafeName, "cafeName");
        this.currencyDisplay = Objects.requireNonNull(currencyDisplay, "currencyDisplay");
        this.footer = Objects.requireNonNull(footer, "footer");
        this.invoiceNumber = Objects.requireNonNull(invoiceNumber, "invoiceNumber");
        this.sessionId = sessionId;
        this.stationName = Objects.requireNonNull(stationName, "stationName");
        this.stationType = Objects.requireNonNull(stationType, "stationType");
        this.mode = mode;
        this.hourlyRateSnapshot = Objects.requireNonNull(
                hourlyRateSnapshot, "hourlyRateSnapshot");
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.endTime = Objects.requireNonNull(endTime, "endTime");
        this.duration = Objects.requireNonNull(duration, "duration");
        this.gamingAmount = Objects.requireNonNull(gamingAmount, "gamingAmount");
        this.purchasedProducts = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(
                        purchasedProducts, "purchasedProducts")));
        this.productsTotal = Objects.requireNonNull(productsTotal, "productsTotal");
        this.total = Objects.requireNonNull(total, "total");
    }

    public String getCafeName() {
        return cafeName;
    }

    public String getCurrencyDisplay() {
        return currencyDisplay;
    }

    public String getFooter() {
        return footer;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public long getSessionId() {
        return sessionId;
    }

    public String getStationName() {
        return stationName;
    }

    public StationType getStationType() {
        return stationType;
    }

    public SessionMode getMode() {
        return mode;
    }

    public BigDecimal getHourlyRateSnapshot() {
        return hourlyRateSnapshot;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public BigDecimal getGamingAmount() {
        return gamingAmount;
    }

    public List<InvoiceItem> getPurchasedProducts() {
        return purchasedProducts;
    }

    public BigDecimal getProductsTotal() {
        return productsTotal;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
