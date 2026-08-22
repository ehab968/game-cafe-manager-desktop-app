package com.gamecafe.gamecafemanager.data.repository;

import com.gamecafe.gamecafemanager.core.database.Database;
import com.gamecafe.gamecafemanager.core.database.DatabaseException;
import com.gamecafe.gamecafemanager.domain.exception.UserNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.PasswordHash;
import com.gamecafe.gamecafemanager.domain.model.Role;
import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.model.UserAccount;
import com.gamecafe.gamecafemanager.domain.repository.UserRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SQLiteUserRepository implements UserRepository {

    private static final String USER_COLUMNS = "id, username, role, enabled";
    private final Database database;

    public SQLiteUserRepository(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public User create(User user, PasswordHash passwordHash) {
        String sql = "INSERT INTO users(username, role, password_algorithm, password_salt, "
                + "password_iterations, password_hash, enabled) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getRole().name());
            bindPassword(statement, 3, passwordHash);
            statement.setInt(7, user.isEnabled() ? 1 : 0);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("User insert did not return an id");
                }
                return new User(
                        generatedKeys.getLong(1),
                        user.getUsername(),
                        user.getRole(),
                        user.isEnabled());
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not create user", exception);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT " + USER_COLUMNS + " FROM users ORDER BY username COLLATE NOCASE";
        List<User> users = new ArrayList<>();
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
            return users;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load users", exception);
        }
    }

    @Override
    public Optional<User> findById(long id) {
        String sql = "SELECT " + USER_COLUMNS + " FROM users WHERE id = ?";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapUser(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load user " + id, exception);
        }
    }

    @Override
    public Optional<UserAccount> findAccountByUsername(String username) {
        String sql = "SELECT " + USER_COLUMNS + ", password_algorithm, password_salt, "
                + "password_iterations, password_hash FROM users WHERE username = ? COLLATE NOCASE";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new UserAccount(
                        mapUser(resultSet),
                        new PasswordHash(
                                resultSet.getString("password_algorithm"),
                                resultSet.getBytes("password_salt"),
                                resultSet.getInt("password_iterations"),
                                resultSet.getBytes("password_hash"))));
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load authentication account", exception);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ? COLLATE NOCASE LIMIT 1";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not check username", exception);
        }
    }

    @Override
    public int count() {
        return queryCount("SELECT COUNT(*) FROM users");
    }

    @Override
    public int countEnabledAdmins() {
        return queryCount("SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND enabled = 1");
    }

    @Override
    public void updateRole(long id, Role role) {
        String sql = "UPDATE users SET role = ?, "
                + "updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = ?";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role.name());
            statement.setLong(2, id);
            executeRequiredUpdate(statement, id);
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update user role " + id, exception);
        }
    }

    @Override
    public void setEnabled(long id, boolean enabled) {
        String sql = "UPDATE users SET enabled = ?, "
                + "updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = ?";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, enabled ? 1 : 0);
            statement.setLong(2, id);
            executeRequiredUpdate(statement, id);
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update user status " + id, exception);
        }
    }

    @Override
    public void updatePassword(long id, PasswordHash passwordHash) {
        String sql = "UPDATE users SET password_algorithm = ?, password_salt = ?, "
                + "password_iterations = ?, password_hash = ?, "
                + "updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = ?";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPassword(statement, 1, passwordHash);
            statement.setLong(5, id);
            executeRequiredUpdate(statement, id);
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update user password " + id, exception);
        }
    }

    private void bindPassword(
            PreparedStatement statement,
            int startIndex,
            PasswordHash passwordHash) throws SQLException {
        statement.setString(startIndex, passwordHash.getAlgorithm());
        statement.setBytes(startIndex + 1, passwordHash.getSalt());
        statement.setInt(startIndex + 2, passwordHash.getIterations());
        statement.setBytes(startIndex + 3, passwordHash.getHash());
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                Role.valueOf(resultSet.getString("role")),
                resultSet.getInt("enabled") == 1);
    }

    private int queryCount(String sql) {
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not count users", exception);
        }
    }

    private void executeRequiredUpdate(PreparedStatement statement, long id) throws SQLException {
        if (statement.executeUpdate() == 0) {
            throw new UserNotFoundException(id);
        }
    }
}
