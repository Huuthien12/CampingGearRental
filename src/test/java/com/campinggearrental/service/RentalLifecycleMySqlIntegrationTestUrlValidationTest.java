package com.campinggearrental.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RentalLifecycleMySqlIntegrationTestUrlValidationTest {
    @Test void acceptsTestDatabaseUrlWithJdbcParameters() {
        assertTrue(RentalLifecycleMySqlIntegrationTest.isTestJdbcUrl(
                "jdbc:mysql://127.0.0.1:3306/camping_gear_rental_test?serverTimezone=UTC"));
    }

    @Test void rejectsApplicationDatabaseUrl() {
        assertFalse(RentalLifecycleMySqlIntegrationTest.isTestJdbcUrl(
                "jdbc:mysql://127.0.0.1:3306/camping_gear_rental?serverTimezone=UTC"));
    }
}
