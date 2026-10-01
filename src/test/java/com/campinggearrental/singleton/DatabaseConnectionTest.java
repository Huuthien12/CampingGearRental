package com.campinggearrental.singleton;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class DatabaseConnectionTest {
    @Test
    void returnsTheSameSingletonInstance() {
        assertSame(DatabaseConnection.getInstance(), DatabaseConnection.getInstance());
    }
}
