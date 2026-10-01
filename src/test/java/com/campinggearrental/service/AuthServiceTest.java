package com.campinggearrental.service;

import com.campinggearrental.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {
    @Test
    void acceptsValidCredentialsAndTrimsUsername() throws SQLException {
        UserRepository repository = (username, password) ->
                username.equals("admin") && password.equals("admin123");
        AuthService service = new AuthService(repository);

        assertTrue(service.login(" admin ", "admin123"));
    }

    @Test
    void rejectsMissingCredentialsWithoutQueryingRepository() throws SQLException {
        UserRepository repository = (username, password) -> {
            throw new AssertionError("Repository must not be called for missing credentials");
        };
        AuthService service = new AuthService(repository);

        assertFalse(service.login("  ", "password"));
        assertFalse(service.login("admin", ""));
        assertFalse(service.login("admin", "  "));
    }

    @Test
    void returnsFalseForInvalidCredentials() throws SQLException {
        AuthService service = new AuthService((username, password) -> false);

        assertFalse(service.login("admin", "wrong-password"));
    }
}
