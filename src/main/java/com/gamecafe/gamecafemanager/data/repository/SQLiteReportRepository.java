package com.gamecafe.gamecafemanager.data.repository;

import com.gamecafe.gamecafemanager.core.database.Database;
import com.gamecafe.gamecafemanager.domain.model.CompletedSessionsReport;
import com.gamecafe.gamecafemanager.domain.model.ProductSales;
import com.gamecafe.gamecafemanager.domain.model.ReportPeriod;
import com.gamecafe.gamecafemanager.domain.model.StationUsage;
import com.gamecafe.gamecafemanager.domain.repository.ReportRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds reports exclusively from persisted completed-session snapshots.
 */
public final class SQLiteReportRepository implements ReportRepository {

    private static final int MONEY_SCALE = 2;
    private static final String PERIOD_FILTER =
            "status = 'COMPLETED' AND end_time IS NOT NULL "
                    + "AND julianday(end_time) >= julianday(?) "
                    + "AND julianday(end_time) < julianday(?)";

    private static final String SUMMARY_SQL =
            "SELECT COUNT(*) AS completed_sessions, "
                    + "COALESCE(SUM(station_total_minor), 0) AS gaming_revenue_minor, "
                    + "COALESCE(SUM(products_total_minor), 0) AS products_revenue_minor, "
                    + "COALESCE(SUM(final_total_minor), 0) AS total_revenue_minor, "
                    + "COALESCE(SUM(CAST(ROUND((julianday(end_time) "
                    + "- julianday(start_time)) * 86400.0) AS INTEGER)), 0) "
                    + "AS total_duration_seconds "
                    + "FROM sessions WHERE " + PERIOD_FILTER;

    private static final String STATION_USAGE_SQL =
            "WITH filtered_sessions AS ("
                    + "SELECT id, station_id, station_name, start_time, end_time "
                    + "FROM sessions WHERE " + PERIOD_FILTER + "), "
                    + "latest_stations AS ("
                    + "SELECT current.station_id, current.station_name "
                    + "FROM filtered_sessions current "
                    + "WHERE current.id = ("
                    + "SELECT recent.id FROM filtered_sessions recent "
                    + "WHERE recent.station_id = current.station_id "
                    + "ORDER BY julianday(recent.end_time) DESC, recent.id DESC LIMIT 1)) "
                    + "SELECT sessions.station_id, latest.station_name, "
                    + "COUNT(*) AS completed_sessions, "
                    + "COALESCE(SUM(CAST(ROUND((julianday(sessions.end_time) "
                    + "- julianday(sessions.start_time)) * 86400.0) AS INTEGER)), 0) "
                    + "AS total_duration_seconds "
                    + "FROM filtered_sessions sessions "
                    + "JOIN latest_stations latest ON latest.station_id = sessions.station_id "
                    + "GROUP BY sessions.station_id, latest.station_name "
                    + "ORDER BY completed_sessions DESC, total_duration_seconds DESC, "
                    + "latest.station_name COLLATE NOCASE";

    private static final String PRODUCT_SALES_SQL =
            "WITH filtered_sessions AS ("
                    + "SELECT id, end_time FROM sessions WHERE " + PERIOD_FILTER + "), "
                    + "filtered_products AS ("
                    + "SELECT items.id, items.product_id, items.product_name, items.quantity, "
                    + "items.line_total_minor, sessions.end_time "
                    + "FROM session_products items "
                    + "JOIN filtered_sessions sessions ON sessions.id = items.session_id), "
                    + "latest_products AS ("
                    + "SELECT current.product_id, current.product_name "
                    + "FROM filtered_products current "
                    + "WHERE current.id = ("
                    + "SELECT recent.id FROM filtered_products recent "
                    + "WHERE recent.product_id = current.product_id "
                    + "ORDER BY julianday(recent.end_time) DESC, recent.id DESC LIMIT 1)) "
                    + "SELECT items.product_id, latest.product_name, "
                    + "SUM(items.quantity) AS quantity_sold, "
                    + "SUM(items.line_total_minor) AS revenue_minor "
                    + "FROM filtered_products items "
                    + "JOIN latest_products latest ON latest.product_id = items.product_id "
                    + "GROUP BY items.product_id, latest.product_name "
                    + "ORDER BY quantity_sold DESC, revenue_minor DESC, "
                    + "latest.product_name COLLATE NOCASE";

    private final Database database;

    public SQLiteReportRepository(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public CompletedSessionsReport getCompletedSessionsReport(ReportPeriod period) {
        Objects.requireNonNull(period, "period");
        return database.executeInTransaction(connection -> {
            Summary summary = loadSummary(connection, period);
            List<StationUsage> stationUsage = loadStationUsage(connection, period);
            List<ProductSales> productSales = loadProductSales(connection, period);
            return new CompletedSessionsReport(
                    period,
                    summary.completedSessions,
                    fromMinorUnits(summary.gamingRevenueMinor),
                    fromMinorUnits(summary.productsRevenueMinor),
                    fromMinorUnits(summary.totalRevenueMinor),
                    averageDuration(summary.totalDurationSeconds, summary.completedSessions),
                    stationUsage,
                    productSales);
        });
    }

    private Summary loadSummary(Connection connection, ReportPeriod period) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SUMMARY_SQL)) {
            bindPeriod(statement, period);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Could not calculate report summary");
                }
                return new Summary(
                        resultSet.getLong("completed_sessions"),
                        resultSet.getLong("gaming_revenue_minor"),
                        resultSet.getLong("products_revenue_minor"),
                        resultSet.getLong("total_revenue_minor"),
                        resultSet.getLong("total_duration_seconds"));
            }
        }
    }

    private List<StationUsage> loadStationUsage(
            Connection connection,
            ReportPeriod period) throws SQLException {
        List<StationUsage> usage = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(STATION_USAGE_SQL)) {
            bindPeriod(statement, period);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    usage.add(new StationUsage(
                            resultSet.getLong("station_id"),
                            resultSet.getString("station_name"),
                            resultSet.getLong("completed_sessions"),
                            Duration.ofSeconds(resultSet.getLong("total_duration_seconds"))));
                }
            }
        }
        return usage;
    }

    private List<ProductSales> loadProductSales(
            Connection connection,
            ReportPeriod period) throws SQLException {
        List<ProductSales> sales = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(PRODUCT_SALES_SQL)) {
            bindPeriod(statement, period);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    sales.add(new ProductSales(
                            resultSet.getLong("product_id"),
                            resultSet.getString("product_name"),
                            resultSet.getLong("quantity_sold"),
                            fromMinorUnits(resultSet.getLong("revenue_minor"))));
                }
            }
        }
        return sales;
    }

    private void bindPeriod(PreparedStatement statement, ReportPeriod period) throws SQLException {
        statement.setString(1, period.getStartInclusive().toString());
        statement.setString(2, period.getEndExclusive().toString());
    }

    private Duration averageDuration(long totalSeconds, long completedSessions) {
        if (completedSessions == 0L) {
            return Duration.ZERO;
        }
        long averageSeconds = BigDecimal.valueOf(totalSeconds)
                .divide(BigDecimal.valueOf(completedSessions), 0, RoundingMode.HALF_UP)
                .longValueExact();
        return Duration.ofSeconds(averageSeconds);
    }

    private BigDecimal fromMinorUnits(long value) {
        return BigDecimal.valueOf(value, MONEY_SCALE);
    }

    private static final class Summary {

        private final long completedSessions;
        private final long gamingRevenueMinor;
        private final long productsRevenueMinor;
        private final long totalRevenueMinor;
        private final long totalDurationSeconds;

        private Summary(
                long completedSessions,
                long gamingRevenueMinor,
                long productsRevenueMinor,
                long totalRevenueMinor,
                long totalDurationSeconds) {
            this.completedSessions = completedSessions;
            this.gamingRevenueMinor = gamingRevenueMinor;
            this.productsRevenueMinor = productsRevenueMinor;
            this.totalRevenueMinor = totalRevenueMinor;
            this.totalDurationSeconds = totalDurationSeconds;
        }
    }
}
