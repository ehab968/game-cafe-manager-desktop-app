package com.gamecafe.gamecafemanager.data.repository;

import com.gamecafe.gamecafemanager.core.database.Database;
import com.gamecafe.gamecafemanager.core.database.DatabaseException;
import com.gamecafe.gamecafemanager.domain.exception.ActiveSessionAlreadyExistsException;
import com.gamecafe.gamecafemanager.domain.exception.SessionNotActiveException;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
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
            "id, station_id, station_name, station_rate_minor, start_time, end_time, "
                    + "status, station_total_minor, products_total_minor, final_total_minor";

    private final Database database;

    public SQLiteSessionRepository(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public Session create(Session session) {
        String sql = "INSERT INTO sessions("
                + "station_id, station_name, station_rate_minor, start_time, end_time, status, "
                + "station_total_minor, products_total_minor, final_total_minor) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, session.getStationId());
            statement.setString(2, session.getStationNameSnapshot());
            statement.setLong(3, toMinorUnits(session.getHourlyRateSnapshot()));
            statement.setString(4, session.getStartTime().toString());
            statement.setString(5, null);
            statement.setString(6, session.getStatus().name());
            statement.setLong(7, toMinorUnits(session.getPlayCost()));
            statement.setLong(8, toMinorUnits(session.getProductsCost()));
            statement.setLong(9, toMinorUnits(session.getFinalTotal()));
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

        String sql = "UPDATE sessions SET end_time = ?, status = ?, station_total_minor = ?, "
                + "products_total_minor = ?, final_total_minor = ?, "
                + "updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') "
                + "WHERE id = ? AND status = 'ACTIVE'";

        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, completedSession.getEndTime().toString());
            statement.setString(2, completedSession.getStatus().name());
            statement.setLong(3, toMinorUnits(completedSession.getPlayCost()));
            statement.setLong(4, toMinorUnits(completedSession.getProductsCost()));
            statement.setLong(5, toMinorUnits(completedSession.getFinalTotal()));
            statement.setLong(6, completedSession.getId());
            if (statement.executeUpdate() == 0) {
                throw new SessionNotActiveException(completedSession.getId());
            }
            return completedSession;
        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Could not finish session " + completedSession.getId(), exception);
        }
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
        return new Session(
                resultSet.getLong("id"),
                resultSet.getLong("station_id"),
                resultSet.getString("station_name"),
                Instant.parse(resultSet.getString("start_time")),
                endTime == null ? null : Instant.parse(endTime),
                SessionStatus.valueOf(resultSet.getString("status")),
                fromMinorUnits(resultSet.getLong("station_rate_minor")),
                fromMinorUnits(resultSet.getLong("station_total_minor")),
                fromMinorUnits(resultSet.getLong("products_total_minor")),
                fromMinorUnits(resultSet.getLong("final_total_minor")));
    }

    private Session copyWithId(Session session, long id) {
        return new Session(
                id,
                session.getStationId(),
                session.getStationNameSnapshot(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus(),
                session.getHourlyRateSnapshot(),
                session.getPlayCost(),
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
