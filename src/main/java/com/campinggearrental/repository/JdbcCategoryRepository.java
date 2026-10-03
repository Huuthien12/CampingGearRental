package com.campinggearrental.repository;

import com.campinggearrental.model.Category;
import com.campinggearrental.singleton.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCategoryRepository implements CategoryRepository {
    @Override
    public List<Category> findAll() throws SQLException {
        List<Category> categories = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT id, name FROM categories ORDER BY name");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) categories.add(read(rows));
        }
        return categories;
    }

    @Override
    public Optional<Category> findById(String id) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT id, name FROM categories WHERE id = ?")) {
            statement.setString(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(read(rows)) : Optional.empty();
            }
        }
    }

    private Category read(ResultSet rows) throws SQLException {
        return new Category(rows.getString("id"), rows.getString("name"));
    }
}
