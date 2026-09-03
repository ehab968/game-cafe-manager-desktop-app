package com.gamecafe.gamecafemanager.data.repository;

import com.gamecafe.gamecafemanager.core.database.Database;
import com.gamecafe.gamecafemanager.core.database.DatabaseException;
import com.gamecafe.gamecafemanager.domain.exception.InsufficientStockException;
import com.gamecafe.gamecafemanager.domain.exception.ProductDisabledException;
import com.gamecafe.gamecafemanager.domain.exception.ProductNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.ProductSale;
import com.gamecafe.gamecafemanager.domain.repository.ProductSaleRepository;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SQLite product-sale repository. Stock reservation and historic sale snapshots
 * are stored in one transaction.
 */
public final class SQLiteProductSaleRepository implements ProductSaleRepository {

    private static final int MONEY_SCALE = 2;
    private static final String SELECT_COLUMNS =
            "id, product_id, product_name, unit_price_minor, quantity, "
                    + "line_total_minor, sold_at";

    private final Database database;

    public SQLiteProductSaleRepository(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public ProductSale sell(long productId, int quantity, Instant soldAt) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        Objects.requireNonNull(soldAt, "soldAt");
        return database.executeInTransaction(connection -> {
            reserveStock(connection, productId, quantity);
            ProductSnapshot product = loadProductSnapshot(connection, productId);
            long lineTotalMinor = Math.multiplyExact(
                    product.unitPriceMinor, (long) quantity);
            return insertSale(
                    connection,
                    productId,
                    product,
                    quantity,
                    lineTotalMinor,
                    soldAt);
        });
    }

    @Override
    public List<ProductSale> findAll() {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM product_sales ORDER BY julianday(sold_at), id";
        List<ProductSale> sales = new ArrayList<>();
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                sales.add(mapSale(resultSet));
            }
            return sales;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load product sales", exception);
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
                throwProductAvailabilityFailure(
                        connection, productId, quantity);
            }
        }
    }

    private void throwProductAvailabilityFailure(
            Connection connection,
            long productId,
            int requestedQuantity) throws SQLException {
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
                        productId,
                        requestedQuantity,
                        resultSet.getInt("stock_quantity"));
            }
        }
    }

    private ProductSnapshot loadProductSnapshot(
            Connection connection,
            long productId) throws SQLException {
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

    private ProductSale insertSale(
            Connection connection,
            long productId,
            ProductSnapshot product,
            int quantity,
            long lineTotalMinor,
            Instant soldAt) throws SQLException {
        String sql = "INSERT INTO product_sales("
                + "product_id, product_name, unit_price_minor, quantity, "
                + "line_total_minor, sold_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, productId);
            statement.setString(2, product.name);
            statement.setLong(3, product.unitPriceMinor);
            statement.setInt(4, quantity);
            statement.setLong(5, lineTotalMinor);
            statement.setString(6, soldAt.toString());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Product sale insert did not return an id");
                }
                return new ProductSale(
                        generatedKeys.getLong(1),
                        productId,
                        product.name,
                        fromMinorUnits(product.unitPriceMinor),
                        quantity,
                        fromMinorUnits(lineTotalMinor),
                        soldAt);
            }
        }
    }

    private ProductSale mapSale(ResultSet resultSet) throws SQLException {
        return new ProductSale(
                resultSet.getLong("id"),
                resultSet.getLong("product_id"),
                resultSet.getString("product_name"),
                fromMinorUnits(resultSet.getLong("unit_price_minor")),
                resultSet.getInt("quantity"),
                fromMinorUnits(resultSet.getLong("line_total_minor")),
                Instant.parse(resultSet.getString("sold_at")));
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
}
