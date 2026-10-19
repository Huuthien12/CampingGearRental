package com.campinggearrental.repository;

import com.campinggearrental.model.Customer;
import com.campinggearrental.model.User;
import java.sql.SQLException;
import java.util.Optional;

public interface UserRepository {
    boolean authenticate(String username, String password) throws SQLException;
    default Optional<User> findByUsername(String username) throws SQLException { return Optional.empty(); }
    default void registerCustomer(Customer customer, User user) throws SQLException {
        throw new UnsupportedOperationException("Customer registration is not supported");
    }
}
