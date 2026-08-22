package com.gamecafe.gamecafemanager.data.repository;

import com.gamecafe.gamecafemanager.core.database.Database;
import com.gamecafe.gamecafemanager.core.database.DatabaseException;
import com.gamecafe.gamecafemanager.domain.exception.InsufficientStockException;
import com.gamecafe.gamecafemanager.domain.exception.ProductDisabledException;
import com.gamecafe.gamecafemanager.domain.exception.ProductNotFoundException;
import com.gamecafe.gamecafemanager.domain.exception.SessionNotActiveException;
import com.gamecafe.gamecafemanager.domain.model.SessionProduct;
import com.gamecafe.gamecafemanager.domain.repository.SessionProductRepository;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SQLite session-product repository. Stock, item snapshots, and session totals
 * are mutated in one transaction.
 */
public final class SQLiteSessionProductRepository implements SessionProductRepository {

    private static final int MONEY_SCALE = 2;
    private static final String SELECT_COLUMNS =
            "id, session_id, product_id, product_name, unit_price_minor, quantity, "
                    + "line_total_minor";

    private final Database database;

    public SQLiteSessionProductRepository(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public SessionProduct addToActiveSession(long sessionId, long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        return database.executeInTransaction(connection -> {
            reserveStock(connection, productId, quantity);
            ProductSnapshot product = loadProductSnapshot(connection, productId);
            SessionTotals totals = loadActiveSessionTotals(connection, sessionId);
            long lineTotalMinor = Math.multiplyExact(product.unitPriceMinor, (long) quantity);
            long productsTotalMinor = Math.addExact(totals.productsTotalMinor, lineTotalMinor);
            long finalTotalMinor = Math.addExact(totals.stationTotalMinor, productsTotalMinor);
            SessionProduct item = insertSessionProduct(
                    connection, sessionId, productId, product, quantity, lineTotalMinor);
            updateSessionTotals(
                    connection, sessionId, productsTotalMinor, finalTotalMinor);
            return item;
        });
    }

    @Override
    public List<SessionProduct> findBySessionId(long sessionId) {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM session_products WHERE session_id = ? ORDER BY id";
        List<SessionProduct> items = new ArrayList<>();
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(mapSessionProduct(resultSet));
                }
            }
            return items;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load products for session " + sessionId, exception);
        }
    }

    private void reserveStock(Connection connection, long productId, int quantity)
            throws SQLException {
        String sql = "UPDATE products SET stock_quantity = stock_quantity - ?, "
                + "updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') "
                + "WHERE id = ? AND active = 1 AND stock_quantity >= ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quantity);
            statement.setLong(2, productId);
            statement.setInt(3, quantity);
            if (statement.executeUpdate() == 0) {
                throwProductAvailabilityFailure(connection, productId, quantity);
            }
        }
    }

    private void throwProductAvailabilityFailure(
            Connection connection, long productId, int requestedQuantity) throws SQLException {
        String sql = "SELECT stock_quantity, active FROM products WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new ProductNotFoundException(productId);
                }
                if (resultSet.getInt("active") != 1) {
                    throw new ProductDisabledException(productId);
                }
                throw new InsufficientStockException(
                        productId, requestedQuantity, resultSet.getInt("stock_quantity"));
            }
        }
    }

    private ProductSnapshot loadProductSnapshot(Connection connection, long productId)
            throws SQLException {
        String sql = "SELECT name, current_price_minor FROM products WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new ProductNotFoundException(productId);
                }
                return new ProductSnapshot(
                        resultSet.getString("name"),
                        resultSet.getLong("current_price_minor"));
            }
        }
    }

    private SessionTotals loadActiveSessionTotals(Connection connection, long sessionId)
            throws SQLException {
        String sql = "SELECT station_total_minor, products_total_minor "
                + "FROM sessions WHERE id = ? AND status = 'ACTIVE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SessionNotActiveException(sessionId);
                }
                return new SessionTotals(
                        resultSet.getLong("station_total_minor"),
                        resultSet.getLong("products_total_minor"));
            }
        }
    }

    private SessionProduct insertSessionProduct(
            Connection connection,
            long sessionId,
            long productId,
            ProductSnapshot product,
            int quantity,
            long lineTotalMinor) throws SQLException {
        String sql = "INSERT INTO session_products("
                + "session_id, product_id, product_name, quantity, unit_price_minor, "
                + "line_total_minor) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, sessionId);
            statement.setLong(2, productId);
            statement.setString(3, product.name);
            statement.setInt(4, quantity);
            statement.setLong(5, product.unitPriceMinor);
            statement.setLong(6, lineTotalMinor);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Session product insert did not return an id");
                }
                return new SessionProduct(
                        generatedKeys.getLong(1),
                        sessionId,
                        productId,
                        product.name,
                        fromMinorUnits(product.unitPriceMinor),
                        quantity,
                        fromMinorUnits(lineTotalMinor));
            }
        }
    }

    private void updateSessionTotals(
            Connection connection,
            long sessionId,
            long productsTotalMinor,
            long finalTotalMinor) throws SQLException {
        String sql = "UPDATE sessions SET "
                + "products_total_minor = ?, final_total_minor = ?, "
                + "updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') "
                + "WHERE id = ? AND status = 'ACTIVE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productsTotalMinor);
            statement.setLong(2, finalTotalMinor);
            statement.setLong(3, sessionId);
            if (statement.executeUpdate() == 0) {
                throw new SessionNotActiveException(sessionId);
            }
        }
    }

    private SessionProduct mapSessionProduct(ResultSet resultSet) throws SQLException {
        return new SessionProduct(
                resultSet.getLong("id"),
                resultSet.getLong("session_id"),
                resultSet.getLong("product_id"),
                resultSet.getString("product_name"),
                fromMinorUnits(resultSet.getLong("unit_price_minor")),
                resultSet.getInt("quantity"),
                fromMinorUnits(resultSet.getLong("line_total_minor")));
    }

    private BigDecimal fromMinorUnits(long value) {
        return BigDecimal.valueOf(value, MONEY_SCALE);
    }

    private static final class ProductSnapshot {

        private final String name;
        private final long unitPriceMinor;

        private ProductSnapshot(String name, long unitPriceMinor) {
            this.name = name;
            this.unitPriceMinor = unitPriceMinor;
        }
    }

    private static final class SessionTotals {

        private final long stationTotalMinor;
        private final long productsTotalMinor;

        private SessionTotals(long stationTotalMinor, long productsTotalMinor) {
            this.stationTotalMinor = stationTotalMinor;
            this.productsTotalMinor = productsTotalMinor;
        }
    }
}
