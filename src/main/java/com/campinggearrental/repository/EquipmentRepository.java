package com.campinggearrental.repository;

import com.campinggearrental.model.Equipment;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface EquipmentRepository {
    List<Equipment> findAll() throws SQLException;
    Optional<Equipment> findById(String id) throws SQLException;
    default Optional<Equipment> findById(Connection connection, String id) throws SQLException { return findById(id); }
    List<Equipment> search(String keyword) throws SQLException;
    void insert(Equipment equipment) throws SQLException;
    void update(Equipment equipment) throws SQLException;
    default void update(Connection connection, Equipment equipment) throws SQLException { update(equipment); }
}
