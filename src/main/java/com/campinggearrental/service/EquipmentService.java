package com.campinggearrental.service;

import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.repository.EquipmentRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class EquipmentService {
    private final EquipmentRepository repository;
    public EquipmentService(EquipmentRepository repository) { this.repository = Objects.requireNonNull(repository); }
    public List<Equipment> list() throws SQLException { return repository.findAll(); }
    public List<Equipment> search(String keyword) throws SQLException { return keyword == null || keyword.isBlank() ? list() : repository.search(keyword.trim()); }
    public void create(Equipment equipment) throws SQLException { repository.insert(Objects.requireNonNull(equipment)); }
    public void update(Equipment equipment) throws SQLException { repository.update(Objects.requireNonNull(equipment)); }
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
