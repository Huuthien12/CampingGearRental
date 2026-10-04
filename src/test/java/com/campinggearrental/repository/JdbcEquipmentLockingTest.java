package com.campinggearrental.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JdbcEquipmentLockingTest {
    @Test void ordinaryReadRemainsUnlockedAndLeavesConnectionOpen() throws Exception {
        JdbcDouble jdbc = new JdbcDouble();
        jdbc.autoCommit = true;
        assertEquals("EQ1", new JdbcEquipmentRepository().findById(jdbc.connection, "EQ1").orElseThrow().getEquipmentId());
        assertFalse(jdbc.sql.contains("FOR UPDATE"));
        assertEquals("EQ1", jdbc.id);
        assertClosedResourcesButNotConnection(jdbc);
    }

    @Test void lockingReadUsesPreparedForUpdateOnCallerConnection() throws Exception {
        JdbcDouble jdbc = new JdbcDouble();
        assertEquals(3, new JdbcEquipmentRepository().findByIdForUpdate(jdbc.connection, "EQ1").orElseThrow().getAvailableQuantity());
        assertTrue(jdbc.sql.endsWith("WHERE id = ? FOR UPDATE"));
        assertEquals("EQ1", jdbc.id);
        assertClosedResourcesButNotConnection(jdbc);
    }

    @Test void lockingReadRejectsAutoCommitInsteadOfPretendingToHoldALock() {
        JdbcDouble jdbc = new JdbcDouble();
        jdbc.autoCommit = true;
        assertThrows(SQLException.class, () -> new JdbcEquipmentRepository().findByIdForUpdate(jdbc.connection, "EQ1"));
        assertNull(jdbc.sql);
        assertFalse(jdbc.connectionClosed);
    }

    @Test void missingRowReturnsEmptyAndStillClosesQueryResources() throws Exception {
        JdbcDouble jdbc = new JdbcDouble();
        jdbc.hasRow = false;
        assertTrue(new JdbcEquipmentRepository().findByIdForUpdate(jdbc.connection, "missing").isEmpty());
        assertClosedResourcesButNotConnection(jdbc);
    }

    private void assertClosedResourcesButNotConnection(JdbcDouble jdbc) {
        assertTrue(jdbc.statementClosed);
        assertTrue(jdbc.resultClosed);
        assertFalse(jdbc.connectionClosed);
    }

    private static class JdbcDouble {
        boolean autoCommit, hasRow = true, statementClosed, resultClosed, connectionClosed;
        String sql, id;
        final Map<String, Object> row = Map.of("id", "EQ1", "name", "Tent", "category_id", "CAT",
                "price_per_day", new BigDecimal("100"), "total_quantity", 5, "available_quantity", 3, "status", "AVAILABLE");
        final ResultSet results = (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "next" -> hasRow;
                    case "getString", "getInt", "getBigDecimal" -> row.get(arguments[0]);
                    case "close" -> { resultClosed = true; yield null; }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        final PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class}, (proxy, method, arguments) -> switch (method.getName()) {
                    case "setString" -> { assertEquals(1, arguments[0]); id = (String) arguments[1]; yield null; }
                    case "executeQuery" -> results;
                    case "close" -> { statementClosed = true; yield null; }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        final Connection connection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getAutoCommit" -> autoCommit;
                    case "prepareStatement" -> { sql = (String) arguments[0]; yield statement; }
                    case "close" -> { connectionClosed = true; yield null; }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
