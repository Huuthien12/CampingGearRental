package com.campinggearrental.repository;

import com.campinggearrental.model.Customer;
import com.campinggearrental.singleton.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCustomerRepository implements CustomerRepository {
    @Override
    public List<Customer> findAll() throws SQLException {
        return query("SELECT id, full_name, phone, email, address FROM customers ORDER BY full_name", null);
    }

    @Override
    public List<Customer> search(String keyword) throws SQLException {
        String sql = "SELECT id, full_name, phone, email, address FROM customers "
                + "WHERE full_name LIKE ? OR phone LIKE ? ORDER BY full_name";
        String pattern = "%" + keyword + "%";
        return query(sql, statement -> {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
        });
    }

    @Override
    public Optional<Customer> findByPhone(String phone) throws SQLException {
        String sql = "SELECT id, full_name, phone, email, address FROM customers WHERE phone = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, phone);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(readCustomer(results)) : Optional.empty();
            }
        }
    }

    @Override
    public void insert(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (id, full_name, phone, email, address) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindCustomer(statement, customer);
            statement.executeUpdate();
        }
    }

    @Override
    public void update(Customer customer) throws SQLException {
        String sql = "UPDATE customers SET full_name = ?, phone = ?, email = ?, address = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customer.fullName());
            statement.setString(2, customer.phone());
            statement.setString(3, customer.email());
            statement.setString(4, customer.address());
            statement.setString(5, customer.id());
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteById(String id) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM customers WHERE id = ?")) {
            statement.setString(1, id);
            statement.executeUpdate();
        }
    }

    private List<Customer> query(String sql, StatementBinder binder) throws SQLException {
        List<Customer> customers = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(statement);
            }
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    customers.add(readCustomer(results));
                }
            }
        }
        return customers;
    }

    private Customer readCustomer(ResultSet results) throws SQLException {
        return new Customer(results.getString("id"), results.getString("full_name"),
                results.getString("phone"), results.getString("email"), results.getString("address"));
    }

    private void bindCustomer(PreparedStatement statement, Customer customer) throws SQLException {
        statement.setString(1, customer.id());
        statement.setString(2, customer.fullName());
        statement.setString(3, customer.phone());
        statement.setString(4, customer.email());
        statement.setString(5, customer.address());
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}