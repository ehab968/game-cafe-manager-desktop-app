package com.gamecafe.gamecafemanager.data.repository;

import com.gamecafe.gamecafemanager.core.database.Database;
import com.gamecafe.gamecafemanager.core.database.DatabaseException;
import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.repository.SettingsRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public final class SQLiteSettingsRepository implements SettingsRepository {

    private final Database database;

    public SQLiteSettingsRepository(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public ApplicationSettings get() {
        ApplicationSettings defaults = ApplicationSettings.defaults();
        return database.executeInTransaction(connection -> {
            insertDefaultsIfMissing(connection, defaults);
            return load(connection);
        });
    }

    @Override
    public ApplicationSettings update(ApplicationSettings settings) {
        String sql = "UPDATE application_settings SET cafe_name = ?, "
                + "currency_display = ?, invoice_footer = ?, "
                + "minimum_session_minutes = ?, billing_rounding_minutes = ?, "
                + "updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = 1";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bindSettings(statement, settings);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Application settings row is missing");
            }
            return settings;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update application settings", exception);
        }
    }

    private void insertDefaultsIfMissing(
            Connection connection,
            ApplicationSettings defaults) throws SQLException {
        String sql = "INSERT OR IGNORE INTO application_settings("
                + "id, cafe_name, currency_display, invoice_footer, "
                + "minimum_session_minutes, billing_rounding_minutes) "
                + "VALUES (1, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindSettings(statement, defaults);
            statement.executeUpdate();
        }
    }

    private ApplicationSettings load(Connection connection) throws SQLException {
        String sql = "SELECT cafe_name, currency_display, invoice_footer, "
                + "minimum_session_minutes, billing_rounding_minutes "
                + "FROM application_settings WHERE id = 1";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new SQLException("Application settings could not be initialized");
            }
            return new ApplicationSettings(
                    resultSet.getString("cafe_name"),
                    resultSet.getString("currency_display"),
                    resultSet.getString("invoice_footer"),
                    nullableInteger(resultSet, "minimum_session_minutes"),
                    nullableInteger(resultSet, "billing_rounding_minutes"));
        }
    }

    private void bindSettings(
            PreparedStatement statement,
            ApplicationSettings settings) throws SQLException {
        statement.setString(1, settings.getCafeName());
        statement.setString(2, settings.getCurrencyDisplay());
        statement.setString(3, settings.getInvoiceFooter());
        bindNullableInteger(statement, 4, settings.getMinimumSessionMinutes());
        bindNullableInteger(statement, 5, settings.getBillingRoundingMinutes());
    }

    private void bindNullableInteger(
            PreparedStatement statement,
            int index,
            Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private Integer nullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }
}
