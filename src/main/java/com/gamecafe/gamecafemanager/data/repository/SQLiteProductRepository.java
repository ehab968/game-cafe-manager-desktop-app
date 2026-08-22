package com.gamecafe.gamecafemanager.data.repository;

import com.gamecafe.gamecafemanager.core.database.Database;
import com.gamecafe.gamecafemanager.core.database.DatabaseException;
import com.gamecafe.gamecafemanager.domain.exception.ProductNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.Product;
import com.gamecafe.gamecafemanager.domain.repository.ProductRepository;
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
 * JDBC-backed product repository. Prices cross the SQLite boundary as exact
 * integer minor units.
 */
public final class SQLiteProductRepository implements ProductRepository {

    private static final int MONEY_SCALE = 2;
    private static final String SELECT_COLUMNS =
            "id, name, current_price_minor, stock_quantity, active";

    private final Database database;

    public SQLiteProductRepository(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public Product create(Product product) {
        String sql = "INSERT INTO products(name, current_price_minor, stock_quantity, active) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
            bindProduct(statement, product);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Product insert did not return an id");
                }
                return new Product(
                        generatedKeys.getLong(1),
                        product.getName(),
                        product.getCurrentPrice(),
                        product.getStockQuantity(),
                        product.isEnabled());
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not create product", exception);
        }
    }

    @Override
    public Product update(Product product) {
        if (product.getId() == null) {
            throw new IllegalArgumentException("Product id is required for update");
        }
        String sql = "UPDATE products SET name = ?, current_price_minor = ?, "
                + "stock_quantity = ?, active = ?, "
                + "updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = ?";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bindProduct(statement, product);
            statement.setLong(5, product.getId());
            if (statement.executeUpdate() == 0) {
                throw new ProductNotFoundException(product.getId());
            }
            return product;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update product " + product.getId(), exception);
        }
    }

    @Override
    public List<Product> findAll() {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products ORDER BY name COLLATE NOCASE";
        List<Product> products = new ArrayList<>();
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                products.add(mapProduct(resultSet));
            }
            return products;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load products", exception);
        }
    }

    @Override
    public Optional<Product> findById(long id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products WHERE id = ?";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapProduct(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load product " + id, exception);
        }
    }

    @Override
    public boolean existsByName(String name, Long excludedProductId) {
        String sql = "SELECT 1 FROM products WHERE name = ? COLLATE NOCASE"
                + (excludedProductId == null ? "" : " AND id <> ?")
                + " LIMIT 1";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            if (excludedProductId != null) {
                statement.setLong(2, excludedProductId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not check product name", exception);
        }
    }

    @Override
    public void setEnabled(long id, boolean enabled) {
        String sql = "UPDATE products SET active = ?, "
                + "updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = ?";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, enabled ? 1 : 0);
            statement.setLong(2, id);
            executeRequiredUpdate(statement, id);
        } catch (SQLException exception) {
            throw new DatabaseException("Could not change product status " + id, exception);
        }
    }

    @Override
    public void updateStock(long id, int stockQuantity) {
        String sql = "UPDATE products SET stock_quantity = ?, "
                + "updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = ?";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stockQuantity);
            statement.setLong(2, id);
            executeRequiredUpdate(statement, id);
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update product stock " + id, exception);
        }
    }

    private void executeRequiredUpdate(PreparedStatement statement, long id) throws SQLException {
        if (statement.executeUpdate() == 0) {
            throw new ProductNotFoundException(id);
        }
    }

    private void bindProduct(PreparedStatement statement, Product product) throws SQLException {
        statement.setString(1, product.getName());
        statement.setLong(2, toMinorUnits(product.getCurrentPrice()));
        statement.setInt(3, product.getStockQuantity());
        statement.setInt(4, product.isEnabled() ? 1 : 0);
    }

    private Product mapProduct(ResultSet resultSet) throws SQLException {
        return new Product(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                BigDecimal.valueOf(resultSet.getLong("current_price_minor"), MONEY_SCALE),
                resultSet.getInt("stock_quantity"),
                resultSet.getInt("active") == 1);
    }

    private long toMinorUnits(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY)
                .movePointRight(MONEY_SCALE)
                .longValueExact();
    }
}
