package com.campinggearrental.service;

import static org.junit.jupiter.api.Assertions.*;

import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.repository.EquipmentRepository;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EquipmentCatalogTransactionTest {
    @Test void locksAndUpdatesOnSameConnectionThenCommits() throws Exception {
        Fixture f = new Fixture();
        Equipment updated = f.service.updateCatalog(" EQ1 ", command(5));
        assertEquals(List.of("begin", "lock:EQ1", "update", "commit", "restore", "close"), f.tx.events);
        assertSame(f.tx.connection, f.store.lockConnection);
        assertSame(f.tx.connection, f.store.updateConnection);
        assertEquals(3, updated.getAvailableQuantity());
        assertEquals("Updated", updated.getName());
        assertNotSame(f.original, updated);
        assertEquals("Tent", f.original.getName());
        assertTrue(f.tx.autoCommit);
    }

    @Test void staleUiSnapshotCannotOverwriteFreshLockedStock() throws Exception {
        Fixture f = new Fixture();
        Equipment staleUiSnapshot = new Equipment("EQ1", "Tent", "CAT", new BigDecimal("100"), 5, 5, EquipmentStatus.AVAILABLE);
        // The current repository row already reflects two units reserved by RentalService.
        assertEquals(5, staleUiSnapshot.getAvailableQuantity());
        assertEquals(3, f.original.getAvailableQuantity());
        Equipment updated = f.service.updateCatalog("EQ1", command(7));
        assertEquals(7, updated.getTotalQuantity());
        assertEquals(5, updated.getAvailableQuantity());
        assertEquals(2, updated.getTotalQuantity() - updated.getAvailableQuantity());
        assertEquals(5, staleUiSnapshot.getTotalQuantity());
        assertEquals(5, staleUiSnapshot.getAvailableQuantity());
    }

    @Test void metadataEditPreservesAlreadyChangedStock() throws Exception {
        Fixture f = new Fixture();
        f.original.release(1);
        assertEquals(4, f.service.updateCatalog("EQ1", command(5)).getAvailableQuantity());
    }

    @Test void persistenceFailureRollsBackAndDoesNotMutateOriginal() {
        Fixture f = new Fixture();
        f.store.failUpdate = true;
        assertThrows(SQLException.class, () -> f.service.updateCatalog("EQ1", command(7)));
        assertEquals(List.of("begin", "lock:EQ1", "update", "rollback", "restore", "close"), f.tx.events);
        assertSame(f.original, f.store.current);
        assertEquals("Tent", f.original.getName());
        assertEquals(5, f.original.getTotalQuantity());
        assertEquals(3, f.original.getAvailableQuantity());
    }

    @Test void reducingBelowReservedQuantityRollsBackBeforeWriting() {
        Fixture f = new Fixture();
        assertThrows(IllegalStateException.class, () -> f.service.updateCatalog("EQ1", command(1)));
        assertFalse(f.tx.events.contains("update"));
        assertTrue(f.tx.events.contains("rollback"));
        assertEquals(3, f.original.getAvailableQuantity());
        assertEquals(5, f.original.getTotalQuantity());
    }

    @Test void domainValidationStillRejectsInvalidCatalogValues() {
        Fixture f = new Fixture();
        assertThrows(IllegalArgumentException.class, () -> f.service.updateCatalog("EQ1",
                new EquipmentService.CatalogUpdate("Updated", "CAT", BigDecimal.ZERO, 5, EquipmentStatus.AVAILABLE)));
        assertThrows(IllegalArgumentException.class, () -> f.service.updateCatalog("EQ1", command(-1)));
        assertFalse(f.tx.events.contains("update"));
        assertEquals("Tent", f.original.getName());
    }

    private static EquipmentService.CatalogUpdate command(int total) {
        return new EquipmentService.CatalogUpdate("Updated", "CAT", new BigDecimal("150"), total, EquipmentStatus.INACTIVE);
    }

    private static class Fixture {
        final Tx tx = new Tx();
        final Equipment original = new Equipment("EQ1", "Tent", "CAT", new BigDecimal("100"), 5, 3, EquipmentStatus.AVAILABLE);
        final Store store = new Store(original, tx);
        final EquipmentService service = new EquipmentService(store, () -> tx.connection);
    }

    private static class Store implements EquipmentRepository {
        Equipment current;
        final Tx tx;
        Connection lockConnection, updateConnection;
        boolean failUpdate;
        Store(Equipment current, Tx tx) { this.current = current; this.tx = tx; }
        public List<Equipment> findAll() { return List.of(current); }
        public Optional<Equipment> findById(String id) { return Optional.of(current); }
        public Optional<Equipment> findByIdForUpdate(Connection connection, String id) {
            assertFalse(tx.autoCommit);
            lockConnection = connection;
            tx.events.add("lock:" + id);
            return Optional.of(current);
        }
        public List<Equipment> search(String keyword) { return List.of(current); }
        public void insert(Equipment equipment) { fail("Unexpected insert"); }
        public void update(Equipment equipment) { fail("Must use caller-owned transaction"); }
        public void update(Connection connection, Equipment equipment) throws SQLException {
            updateConnection = connection;
            tx.events.add("update");
            if (failUpdate) throw new SQLException("write failed");
            current = equipment;
        }
    }

    private static class Tx {
        boolean autoCommit = true;
        final List<String> events = new ArrayList<>();
        final Connection connection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, arguments) -> switch (method.getName()) {
                    case "getAutoCommit" -> autoCommit;
                    case "setAutoCommit" -> { autoCommit = (boolean) arguments[0]; events.add(autoCommit ? "restore" : "begin"); yield null; }
                    case "commit", "rollback", "close" -> { events.add(method.getName()); yield null; }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
