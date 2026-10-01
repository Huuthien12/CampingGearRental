package com.campinggearrental.repository;

import com.campinggearrental.model.Customer;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    List<Customer> findAll() throws SQLException;

    List<Customer> search(String keyword) throws SQLException;

    Optional<Customer> findByPhone(String phone) throws SQLException;

    void insert(Customer customer) throws SQLException;

    void update(Customer customer) throws SQLException;

    void deleteById(String id) throws SQLException;
}