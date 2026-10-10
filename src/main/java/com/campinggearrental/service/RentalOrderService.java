package com.campinggearrental.service;

import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.repository.RentalOrderRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/** Read-only service for the Rental Web list and detail views. */
public class RentalOrderService {
    private final RentalOrderRepository rentalOrderRepository;

    public RentalOrderService(RentalOrderRepository rentalOrderRepository) {
        this.rentalOrderRepository = Objects.requireNonNull(rentalOrderRepository);
    }

    public List<RentalOrder> findAll() throws SQLException {
        return rentalOrderRepository.findAll();
    }

    public RentalOrder findById(String id) throws SQLException {
        if (id == null || id.isBlank()) return null;
        return rentalOrderRepository.findById(id.trim()).orElse(null);
    }
}
