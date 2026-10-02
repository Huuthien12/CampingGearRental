package com.campinggearrental.repository;

import com.campinggearrental.model.RentalOrder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface RentalOrderRepository {
    void insert(RentalOrder rentalOrder) throws SQLException;
    Optional<RentalOrder> findById(String id) throws SQLException;
    default Optional<RentalOrder> findById(Connection connection, String id) throws SQLException { return findById(id); }
    List<RentalOrder> findAll() throws SQLException;
    void update(RentalOrder rentalOrder) throws SQLException;
    default void update(Connection connection, RentalOrder rentalOrder) throws SQLException { update(rentalOrder); }
}
