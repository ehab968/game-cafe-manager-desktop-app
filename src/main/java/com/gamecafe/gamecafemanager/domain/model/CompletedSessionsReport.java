package com.gamecafe.gamecafemanager.domain.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class CompletedSessionsReport {

    private final ReportPeriod period;
    private final long completedSessionsCount;
    private final BigDecimal gamingRevenue;
    private final BigDecimal productsRevenue;
    private final BigDecimal totalRevenue;
    private final Duration averageSessionDuration;
    private final List<StationUsage> stationUsage;
    private final List<ProductSales> productSales;

    public CompletedSessionsReport(
            ReportPeriod period,
            long completedSessionsCount,
            BigDecimal gamingRevenue,
            BigDecimal productsRevenue,
            BigDecimal totalRevenue,
            Duration averageSessionDuration,
            List<StationUsage> stationUsage,
            List<ProductSales> productSales) {
        this.period = Objects.requireNonNull(period, "period");
        this.completedSessionsCount = completedSessionsCount;
        this.gamingRevenue = Objects.requireNonNull(gamingRevenue, "gamingRevenue");
        this.productsRevenue = Objects.requireNonNull(productsRevenue, "productsRevenue");
        this.totalRevenue = Objects.requireNonNull(totalRevenue, "totalRevenue");
        this.averageSessionDuration = Objects.requireNonNull(
                averageSessionDuration, "averageSessionDuration");
        this.stationUsage = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(stationUsage, "stationUsage")));
        this.productSales = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(productSales, "productSales")));
    }

    public ReportPeriod getPeriod() {
        return period;
    }

    public long getCompletedSessionsCount() {
        return completedSessionsCount;
    }

    public BigDecimal getGamingRevenue() {
        return gamingRevenue;
    }

    public BigDecimal getProductsRevenue() {
        return productsRevenue;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public Duration getAverageSessionDuration() {
        return averageSessionDuration;
    }

    public List<StationUsage> getStationUsage() {
        return stationUsage;
    }

    public List<ProductSales> getProductSales() {
        return productSales;
    }
}
