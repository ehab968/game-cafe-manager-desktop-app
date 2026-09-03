package com.gamecafe.gamecafemanager.data.repository;

import com.gamecafe.gamecafemanager.core.database.Database;
import com.gamecafe.gamecafemanager.core.database.DatabaseException;
import com.gamecafe.gamecafemanager.domain.exception.ActiveSessionAlreadyExistsException;
import com.gamecafe.gamecafemanager.domain.exception.DuplicateCheckoutException;
import com.gamecafe.gamecafemanager.domain.exception.SessionNotActiveException;
import com.gamecafe.gamecafemanager.domain.exception.SessionNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.GamingDiscount;
import com.gamecafe.gamecafemanager.domain.model.SessionMode;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * SQLite implementation of the session repository. All time and price
 * snapshots are written in the same insert that creates the active session.
 */
public final class SQLiteSessionRepository implements SessionRepository {

    private static final int MONEY_SCALE = 2;
    private static final String SELECT_COLUMNS =
            "id, station_id, station_name, station_type, session_mode, station_rate_minor, "
                    + "start_time, end_time, "
                    + "status, station_total_minor, gaming_discount_percent, "
                    + "gaming_discount_minor, discounted_gaming_total_minor, "
                    + "products_total_minor, final_total_minor";

    private final Database database;

    public SQLiteSessionRepository(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public Session create(Session session) {
        String sql = "INSERT INTO sessions("
                + "station_id, station_name, station_type, session_mode, station_rate_minor, "
                + "start_time, end_time, status, "
                + "station_total_minor, gaming_discount_percent, gaming_discount_minor, "
                + "discounted_gaming_total_minor, products_total_minor, final_total_minor) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, session.getStationId());
            statement.setString(2, session.getStationNameSnapshot());
            statement.setString(3, session.getStationTypeSnapshot().name());
            statement.setString(4, session.getMode() == null
                    ? null
                    : session.getMode().name());
            statement.setLong(5, toMinorUnits(session.getHourlyRateSnapshot()));
            statement.setString(6, session.getStartTime().toString());
            statement.setString(7, null);
            statement.setString(8, session.getStatus().name());
            statement.setLong(9, toMinorUnits(session.getPlayCost()));
            statement.setInt(10, session.getGamingDiscount().getPercentage());
            statement.setLong(11, toMinorUnits(session.getGamingDiscountAmount()));
            statement.setLong(12, toMinorUnits(session.getDiscountedPlayCost()));
            statement.setLong(13, toMinorUnits(session.getProductsCost()));
            statement.setLong(14, toMinorUnits(session.getFinalTotal()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Session insert did not return an id");
                }
                return copyWithId(session, keys.getLong(1));
            }
        } catch (SQLException exception) {
            if (isDuplicateActiveSession(exception)) {
                throw new ActiveSessionAlreadyExistsException(session.getStationId());
            }
            throw new DatabaseException("Could not start session", exception);
        }
    }

    @Override
    public Session finish(Session completedSession) {
        if (completedSession.getId() == null) {
            throw new IllegalArgumentException("Session id is required for completion");
        }
        if (completedSession.getEndTime() == null) {
            throw new IllegalArgumentException("Session end time is required for completion");
        }

        return database.executeInTransaction(connection -> {
            long playCostMinor = toMinorUnits(completedSession.getPlayCost());
            long gamingDiscountMinor = toMinorUnits(
                    completedSession.getGamingDiscountAmount());
            long discountedPlayCostMinor = toMinorUnits(
                    completedSession.getDiscountedPlayCost());
            claimActiveSession(
                    connection,
                    completedSession,
                    playCostMinor,
                    gamingDiscountMinor,
                    discountedPlayCostMinor);
            long productsTotalMinor = loadProductsTotal(connection, completedSession.getId());
            long finalTotalMinor = Math.addExact(
                    discountedPlayCostMinor, productsTotalMinor);
            storeCheckoutTotals(
                    connection,
                    completedSession.getId(),
                    productsTotalMinor,
                    finalTotalMinor);
            return new Session(
                    completedSession.getId(),
                    completedSession.getStationId(),
                    completedSession.getStationNameSnapshot(),
                    completedSession.getStationTypeSnapshot(),
                    completedSession.getMode(),
                    completedSession.getStartTime(),
                    completedSession.getEndTime(),
                    completedSession.getStatus(),
                    completedSession.getHourlyRateSnapshot(),
                    fromMinorUnits(playCostMinor),
                    completedSession.getGamingDiscount(),
                    fromMinorUnits(gamingDiscountMinor),
                    fromMinorUnits(discountedPlayCostMinor),
                    fromMinorUnits(productsTotalMinor),
                    fromMinorUnits(finalTotalMinor));
        });
    }

    @Override
    public Optional<Session> findById(long id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM sessions WHERE id = ?";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(mapSession(resultSet))
                        : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load session " + id, exception);
        }
    }

    @Override
    public Optional<Session> findActiveByStationId(long stationId) {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM sessions WHERE station_id = ? AND status = 'ACTIVE'";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, stationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(mapSession(resultSet))
                        : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load active station session", exception);
        }
    }

    @Override
    public List<Session> findActive() {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM sessions WHERE status = 'ACTIVE' ORDER BY start_time";
        List<Session> sessions = new ArrayList<>();

        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                sessions.add(mapSession(resultSet));
            }
            return sessions;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load active sessions", exception);
        }
    }

    private Session mapSession(ResultSet resultSet) throws SQLException {
        String endTime = resultSet.getString("end_time");
        String mode = resultSet.getString("session_mode");
        return new Session(
                resultSet.getLong("id"),
                resultSet.getLong("station_id"),
                resultSet.getString("station_name"),
                StationType.valueOf(resultSet.getString("station_type")),
                mode == null ? null : SessionMode.valueOf(mode),
                Instant.parse(resultSet.getString("start_time")),
                endTime == null ? null : Instant.parse(endTime),
                SessionStatus.valueOf(resultSet.getString("status")),
                fromMinorUnits(resultSet.getLong("station_rate_minor")),
                fromMinorUnits(resultSet.getLong("station_total_minor")),
                GamingDiscount.fromPercentage(
                        resultSet.getInt("gaming_discount_percent")),
                fromMinorUnits(resultSet.getLong("gaming_discount_minor")),
                fromMinorUnits(resultSet.getLong("discounted_gaming_total_minor")),
                fromMinorUnits(resultSet.getLong("products_total_minor")),
                fromMinorUnits(resultSet.getLong("final_total_minor")));
    }

    private void claimActiveSession(
            Connection connection,
            Session completedSession,
            long playCostMinor,
            long gamingDiscountMinor,
            long discountedPlayCostMinor) throws SQLException {
        String sql = "UPDATE sessions SET end_time = ?, status = 'COMPLETED', "
                + "station_total_minor = ?, "
                + "gaming_discount_percent = ?, gaming_discount_minor = ?, "
                + "discounted_gaming_total_minor = ?, "
                + "updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') "
                + "WHERE id = ? AND status = 'ACTIVE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, completedSession.getEndTime().toString());
            statement.setLong(2, playCostMinor);
            statement.setInt(3, completedSession.getGamingDiscount().getPercentage());
            statement.setLong(4, gamingDiscountMinor);
            statement.setLong(5, discountedPlayCostMinor);
            statement.setLong(6, completedSession.getId());
            if (statement.executeUpdate() == 0) {
                throwSessionCompletionFailure(connection, completedSession.getId());
            }
        }
    }

    private void throwSessionCompletionFailure(Connection connection, long sessionId)
            throws SQLException {
        String sql = "SELECT status FROM sessions WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SessionNotFoundException(sessionId);
                }
                if (SessionStatus.COMPLETED.name().equals(resultSet.getString("status"))) {
                    throw new DuplicateCheckoutException(sessionId);
                }
                throw new SessionNotActiveException(sessionId);
            }
        }
    }

    private long loadProductsTotal(Connection connection, long sessionId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(line_total_minor), 0) "
                + "FROM session_products WHERE session_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Could not calculate session product total");
                }
                return resultSet.getLong(1);
            }
        }
    }

    private void storeCheckoutTotals(
            Connection connection,
            long sessionId,
            long productsTotalMinor,
            long finalTotalMinor) throws SQLException {
        String sql = "UPDATE sessions SET products_total_minor = ?, final_total_minor = ? "
                + "WHERE id = ? AND status = 'COMPLETED'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productsTotalMinor);
            statement.setLong(2, finalTotalMinor);
            statement.setLong(3, sessionId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Could not store checkout totals for session " + sessionId);
            }
        }
    }

    private Session copyWithId(Session session, long id) {
        return new Session(
                id,
                session.getStationId(),
                session.getStationNameSnapshot(),
                session.getStationTypeSnapshot(),
                session.getMode(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus(),
                session.getHourlyRateSnapshot(),
                session.getPlayCost(),
                session.getGamingDiscount(),
                session.getGamingDiscountAmount(),
                session.getDiscountedPlayCost(),
                session.getProductsCost(),
                session.getFinalTotal());
    }

    private long toMinorUnits(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY)
                .movePointRight(MONEY_SCALE)
                .longValueExact();
    }

    private BigDecimal fromMinorUnits(long value) {
        return BigDecimal.valueOf(value, MONEY_SCALE);
    }

    private boolean isDuplicateActiveSession(SQLException exception) {
        String message = exception.getMessage();
        return exception.getErrorCode() == 19
                && message != null
                && message.contains("sessions.station_id");
    }
}
