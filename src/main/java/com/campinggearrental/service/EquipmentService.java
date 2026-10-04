package com.campinggearrental.service;

import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.repository.EquipmentRepository;
import com.campinggearrental.singleton.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class EquipmentService {
    private final EquipmentRepository repository;
    private final ConnectionProvider connectionProvider;
    public EquipmentService(EquipmentRepository repository) {
        this(repository, DatabaseConnection.getInstance()::getConnection);
    }
    public EquipmentService(EquipmentRepository repository, ConnectionProvider connectionProvider) {
        this.repository = Objects.requireNonNull(repository);
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
    }
    public List<Equipment> list() throws SQLException { return repository.findAll(); }
    public Equipment getById(String id) throws SQLException { return require(id); }
    public List<Equipment> search(String keyword) throws SQLException { return keyword == null || keyword.isBlank() ? list() : repository.search(keyword.trim()); }
    public void create(Equipment equipment) throws SQLException { repository.insert(Objects.requireNonNull(equipment)); }
    public void update(Equipment equipment) throws SQLException { repository.update(Objects.requireNonNull(equipment)); }

    public record CatalogUpdate(String name, String categoryId, BigDecimal pricePerDay,
                                int totalQuantity, EquipmentStatus status) { }

    /** Applies catalog changes to freshly locked stock using existing domain validation. */
    public Equipment updateCatalog(String id, CatalogUpdate input) throws SQLException {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("equipment id must not be blank");
        Objects.requireNonNull(input, "catalog update is required");
        if (input.status() == null) throw new IllegalArgumentException("Status is required.");
        try (Connection connection = connectionProvider.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Equipment current = repository.findByIdForUpdate(connection, id.trim())
                        .orElseThrow(() -> new IllegalArgumentException("equipment not found: " + id));
                Equipment updated = new Equipment(current.getEquipmentId(), current.getName(), current.getCategoryId(),
                        current.getPricePerDay(), current.getTotalQuantity(), current.getAvailableQuantity(), current.getStatus());
                updated.setName(input.name());
                updated.setCategoryId(input.categoryId());
                updated.setPricePerDay(input.pricePerDay());
                updated.setStatus(input.status());
                updated.adjustTotalQuantity(input.totalQuantity());
                repository.update(connection, updated);
                connection.commit();
                return updated;
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); }
                catch (SQLException rollbackFailure) { exception.addSuppressed(rollbackFailure); }
                throw exception;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    @FunctionalInterface public interface ConnectionProvider { Connection getConnection() throws SQLException; }
    public boolean hasEnoughStock(String id, int quantity) throws SQLException { return require(id).hasEnoughStock(quantity); }
    public void reserve(String id, int quantity) throws SQLException { change(id, equipment -> equipment.reserve(quantity)); }
    public void release(String id, int quantity) throws SQLException { change(id, equipment -> equipment.release(quantity)); }
    public void adjustTotalQuantity(String id, int totalQuantity) throws SQLException { change(id, equipment -> equipment.adjustTotalQuantity(totalQuantity)); }
    public void setStatus(String id, EquipmentStatus status) throws SQLException { change(id, equipment -> equipment.setStatus(status)); }
    private Equipment require(String id) throws SQLException {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("equipment id must not be blank");
        return repository.findById(id.trim()).orElseThrow(() -> new IllegalArgumentException("equipment not found: " + id));
    }
    private void change(String id, Change change) throws SQLException { Equipment equipment = require(id); change.apply(equipment); repository.update(equipment); }
    @FunctionalInterface private interface Change { void apply(Equipment equipment); }
}
