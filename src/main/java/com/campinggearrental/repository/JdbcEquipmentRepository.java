package com.campinggearrental.repository;

import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.singleton.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcEquipmentRepository implements EquipmentRepository {
    private static final String COLUMNS = "id, name, category_id, price_per_day, total_quantity, available_quantity, status";

    public List<Equipment> findAll() throws SQLException { return query("SELECT " + COLUMNS + " FROM equipment ORDER BY name", null); }

    public Optional<Equipment> findById(String id) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT " + COLUMNS + " FROM equipment WHERE id = ?")) {
            statement.setString(1, id);
            try (ResultSet results = statement.executeQuery()) { return results.next() ? Optional.of(read(results)) : Optional.empty(); }
        }
    }

    public List<Equipment> search(String keyword) throws SQLException {
        String pattern = "%" + keyword + "%";
        return query("SELECT " + COLUMNS + " FROM equipment WHERE name LIKE ? OR category_id LIKE ? ORDER BY name", statement -> {
            statement.setString(1, pattern); statement.setString(2, pattern);
        });
    }

    public void insert(Equipment equipment) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO equipment (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            bind(statement, equipment); statement.executeUpdate();
        }
    }

    public void update(Equipment equipment) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE equipment SET name=?, category_id=?, price_per_day=?, total_quantity=?, available_quantity=?, status=? WHERE id=?")) {
            statement.setString(1, equipment.getName()); statement.setString(2, equipment.getCategoryId());
            statement.setBigDecimal(3, equipment.getPricePerDay()); statement.setInt(4, equipment.getTotalQuantity());
            statement.setInt(5, equipment.getAvailableQuantity()); statement.setString(6, equipment.getStatus().name());
            statement.setString(7, equipment.getEquipmentId()); statement.executeUpdate();
        }
    }

    private List<Equipment> query(String sql, Binder binder) throws SQLException {
        List<Equipment> equipment = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) binder.bind(statement);
            try (ResultSet results = statement.executeQuery()) { while (results.next()) equipment.add(read(results)); }
        }
        return equipment;
    }

    private Equipment read(ResultSet results) throws SQLException {
        return new Equipment(results.getString("id"), results.getString("name"), results.getString("category_id"),
                results.getBigDecimal("price_per_day"), results.getInt("total_quantity"), results.getInt("available_quantity"),
                EquipmentStatus.valueOf(results.getString("status")));
    }

    private void bind(PreparedStatement statement, Equipment equipment) throws SQLException {
        statement.setString(1, equipment.getEquipmentId()); statement.setString(2, equipment.getName());
        statement.setString(3, equipment.getCategoryId()); statement.setBigDecimal(4, equipment.getPricePerDay());
        statement.setInt(5, equipment.getTotalQuantity()); statement.setInt(6, equipment.getAvailableQuantity());
        statement.setString(7, equipment.getStatus().name());
    }

    @FunctionalInterface private interface Binder { void bind(PreparedStatement statement) throws SQLException; }
}
