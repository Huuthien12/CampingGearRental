package com.campinggearrental.repository;

import com.campinggearrental.singleton.DatabaseConnection;
import com.campinggearrental.model.Customer;
import com.campinggearrental.model.User;
import com.campinggearrental.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class JdbcUserRepository implements UserRepository {
    @Override
    public boolean authenticate(String username, String password) throws SQLException {
        return findByUsername(username).filter(user -> new BCryptPasswordEncoder().matches(password, user.password())).isPresent();
    }

    @Override
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password, role, customer_id FROM users WHERE username = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) return Optional.empty();
                return Optional.of(new User(results.getString("id"), results.getString("username"),
                        results.getString("password"), UserRole.valueOf(results.getString("role")),
                        results.getString("customer_id")));
            }
        }
    }

    @Override
    public void registerCustomer(Customer customer, User user) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement customerStatement = connection.prepareStatement(
                    "INSERT INTO customers (id, full_name, phone, email, address) VALUES (?, ?, ?, ?, ?)");
                 PreparedStatement userStatement = connection.prepareStatement(
                    "INSERT INTO users (id, username, password, role, customer_id) VALUES (?, ?, ?, ?, ?)")) {
                customerStatement.setString(1, customer.id());
                customerStatement.setString(2, customer.fullName());
                customerStatement.setString(3, customer.phone());
                customerStatement.setString(4, customer.email());
                customerStatement.setString(5, customer.address());
                customerStatement.executeUpdate();
                userStatement.setString(1, user.id());
                userStatement.setString(2, user.username());
                userStatement.setString(3, user.password());
                userStatement.setString(4, user.role().name());
                userStatement.setString(5, user.customerId());
                userStatement.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
}
