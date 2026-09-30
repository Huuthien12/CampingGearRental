package com.campinggearrental.service;

import com.campinggearrental.repository.UserRepository;

import java.sql.SQLException;

public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean login(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return false;
        }
        return userRepository.authenticate(username.trim(), password);
    }
}