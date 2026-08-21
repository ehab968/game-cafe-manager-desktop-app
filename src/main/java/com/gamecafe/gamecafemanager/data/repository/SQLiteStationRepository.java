package com.gamecafe.gamecafemanager.data.repository;

import com.gamecafe.gamecafemanager.core.database.Database;
import com.gamecafe.gamecafemanager.core.database.DatabaseException;
import com.gamecafe.gamecafemanager.domain.exception.StationNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC-backed station repository. Monetary values cross the SQLite boundary as
 * exact integer minor units.
 */
public final class SQLiteStationRepository implements StationRepository {

    private static final int MONEY_SCALE = 2;
    private static final String SELECT_COLUMNS =
            "id, name, type, hourly_rate_minor, active";

    private final Database database;

    public SQLiteStationRepository(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public Station create(Station station) {
        String sql = "INSERT INTO stations(name, type, hourly_rate_minor, active) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
            bindStation(statement, station);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Station insert did not return an id");
                }
                return new Station(
                        generatedKeys.getLong(1),
                        station.getName(),
                        station.getType(),
                        station.getHourlyRate(),
                        station.isEnabled());
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not create station", exception);
        }
    }

    @Override
    public Station update(Station station) {
        if (station.getId() == null) {
            throw new IllegalArgumentException("Station id is required for update");
        }

        String sql = "UPDATE stations SET name = ?, type = ?, hourly_rate_minor = ?, "
                + "active = ?, updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') "
                + "WHERE id = ?";

        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bindStation(statement, station);
            statement.setLong(5, station.getId());
            if (statement.executeUpdate() == 0) {
                throw new StationNotFoundException(station.getId());
            }
            return station;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update station " + station.getId(), exception);
        }
    }

    @Override
    public List<Station> findAll() {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM stations ORDER BY type, name COLLATE NOCASE";
        List<Station> stations = new ArrayList<>();

        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                stations.add(mapStation(resultSet));
            }
            return stations;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load stations", exception);
        }
    }

    @Override
    public Optional<Station> findById(long id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM stations WHERE id = ?";

        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(mapStation(resultSet))
                        : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load station " + id, exception);
        }
    }

    @Override
    public boolean existsByName(String name, Long excludedStationId) {
        String sql = "SELECT 1 FROM stations WHERE name = ? COLLATE NOCASE"
                + (excludedStationId == null ? "" : " AND id <> ?")
                + " LIMIT 1";

        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            if (excludedStationId != null) {
                statement.setLong(2, excludedStationId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not check station name", exception);
        }
    }

    @Override
    public void setEnabled(long id, boolean enabled) {
        String sql = "UPDATE stations SET active = ?, "
                + "updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = ?";

        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, enabled ? 1 : 0);
            statement.setLong(2, id);
            if (statement.executeUpdate() == 0) {
                throw new StationNotFoundException(id);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not change station status " + id, exception);
        }
    }

    private void bindStation(PreparedStatement statement, Station station) throws SQLException {
        statement.setString(1, station.getName());
        statement.setString(2, station.getType().name());
        statement.setLong(3, toMinorUnits(station.getHourlyRate()));
        statement.setInt(4, station.isEnabled() ? 1 : 0);
    }

    private Station mapStation(ResultSet resultSet) throws SQLException {
        return new Station(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                StationType.valueOf(resultSet.getString("type")),
                BigDecimal.valueOf(resultSet.getLong("hourly_rate_minor"), MONEY_SCALE),
                resultSet.getInt("active") == 1);
    }

    private long toMinorUnits(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY)
                .movePointRight(MONEY_SCALE)
                .longValueExact();
    }
}
