package com.campinggearrental.repository;

import com.campinggearrental.model.RentalOrder;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface RentalOrderRepository {
    void insert(RentalOrder rentalOrder) throws SQLException;
    Optional<RentalOrder> findById(String id) throws SQLException;
    List<RentalOrder> findAll() throws SQLException;
    void update(RentalOrder rentalOrder) throws SQLException;
}
