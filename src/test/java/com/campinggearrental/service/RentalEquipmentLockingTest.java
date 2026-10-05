package com.campinggearrental.service;

import static org.junit.jupiter.api.Assertions.*;

import com.campinggearrental.model.*;
import com.campinggearrental.repository.*;
import com.campinggearrental.state.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;

class RentalEquipmentLockingTest {
    @Test void confirmDetailsABAcquiresABLocks() throws Exception {
        Fixture f = fixture(new PendingState(), false, false);
        f.service.confirmRental("R1");
        assertEquals(List.of("A", "B"), f.store.locks);
        assertEquals(4, f.store.items.get("A").getAvailableQuantity());
        assertEquals(3, f.store.items.get("B").getAvailableQuantity());
    }

    @Test void confirmDetailsBAStillLocksABAndPreservesQuantityMapping() throws Exception {
        Fixture f = fixture(new PendingState(), true, false);
        f.service.confirmRental("R1");
        assertEquals(List.of("A", "B"), f.store.locks);
        assertEquals(List.of("B", "A"), f.order.getDetails().stream().map(RentalDetail::getEquipmentId).toList());
        assertEquals(List.of("B", "A"), f.store.writes);
        assertEquals(4, f.store.items.get("A").getAvailableQuantity());
        assertEquals(3, f.store.items.get("B").getAvailableQuantity());
    }

    @Test void cancelConfirmedUsesLockingReadsAndRestoresMatchingQuantities() throws Exception {
        Fixture f = fixture(new ConfirmedState(), true, true);
        f.service.cancelRental("R1");
        assertEquals(List.of("A", "B"), f.store.locks);
        assertEquals(5, f.store.items.get("A").getAvailableQuantity());
        assertEquals(5, f.store.items.get("B").getAvailableQuantity());
        assertInstanceOf(CancelledState.class, f.order.getCurrentState());
    }

    @Test void returnUsesLockingReadsAndRestoresMatchingQuantities() throws Exception {
        Fixture f = fixture(new RentedState(), true, true);
        f.service.returnRental("R1", LocalDate.of(2026, 10, 4));
        assertEquals(List.of("A", "B"), f.store.locks);
        assertEquals(5, f.store.items.get("A").getAvailableQuantity());
        assertEquals(5, f.store.items.get("B").getAvailableQuantity());
        assertInstanceOf(ReturnedState.class, f.order.getCurrentState());
    }

    @Test void duplicateIdsAreLockedOnceWithoutReorderingDetails() throws Exception {
        Fixture f = fixture(new PendingState(), true, false);
        f.order.setDetails(List.of(detail("B", 2), detail("A", 1), detail("A", 1)));
        f.service.confirmRental("R1");
        assertEquals(List.of("A", "B"), f.store.locks);
        assertEquals(List.of("B", "A", "A"), f.store.writes);
        assertEquals(3, f.store.items.get("A").getAvailableQuantity());
        assertEquals(3, f.store.items.get("B").getAvailableQuantity());
    }

    private static Fixture fixture(RentalState state, boolean reversed, boolean reserved) {
        Tx tx = new Tx();
        Store store = new Store(tx);
        store.items.put("A", new Equipment("A", "Tent", "CAT", BigDecimal.TEN, 5, reserved ? 4 : 5, EquipmentStatus.AVAILABLE));
        store.items.put("B", new Equipment("B", "Bag", "CAT", BigDecimal.ONE, 5, reserved ? 3 : 5, EquipmentStatus.AVAILABLE));
        RentalOrder order = new RentalOrder();
        order.setId("R1"); order.setCurrentState(state);
        order.setRentalDate(LocalDate.of(2026, 10, 1)); order.setExpectedReturnDate(LocalDate.of(2026, 10, 3));
        order.setDetails(reversed ? List.of(detail("B", 2), detail("A", 1)) : List.of(detail("A", 1), detail("B", 2)));
        CustomerRepository customers = new CustomerRepository() {
            public List<Customer> findAll() { return List.of(); }
            public Optional<Customer> findById(String id) { return Optional.empty(); }
            public List<Customer> search(String keyword) { return List.of(); }
            public Optional<Customer> findByPhone(String phone) { return Optional.empty(); }
            public void insert(Customer customer) { }
            public void update(Customer customer) { }
            public void deleteById(String id) { }
        };
        RentalOrderRepository orders = new RentalOrderRepository() {
            public Optional<RentalOrder> findById(String id) { return Optional.of(order); }
            public List<RentalOrder> findAll() { return List.of(order); }
            public void insert(RentalOrder value) { }
            public void update(RentalOrder value) { }
            public void update(Connection connection, RentalOrder value) { assertSame(tx.connection, connection); }
        };
        return new Fixture(new RentalService(customers, store, orders, () -> tx.connection), order, store);
    }

    private static RentalDetail detail(String id, int quantity) {
        RentalDetail detail = new RentalDetail();
        detail.setEquipmentId(id); detail.setQuantity(quantity); detail.setUnitPrice(BigDecimal.TEN);
        return detail;
    }

    private record Fixture(RentalService service, RentalOrder order, Store store) { }

    private static class Store implements EquipmentRepository {
        final Tx tx;
        final Map<String, Equipment> items = new HashMap<>();
        final List<String> locks = new ArrayList<>(), writes = new ArrayList<>();
        Store(Tx tx) { this.tx = tx; }
        public List<Equipment> findAll() { return List.copyOf(items.values()); }
        public Optional<Equipment> findById(String id) { fail("Lifecycle must explicitly lock"); return Optional.empty(); }
        public Optional<Equipment> findByIdForUpdate(Connection connection, String id) {
            assertSame(tx.connection, connection);
            assertFalse(tx.autoCommit);
            locks.add(id);
            return Optional.ofNullable(items.get(id));
        }
        public List<Equipment> search(String keyword) { return List.of(); }
        public void insert(Equipment equipment) { }
        public void update(Equipment equipment) { fail("Lifecycle must use its transaction connection"); }
        public void update(Connection connection, Equipment equipment) {
            assertSame(tx.connection, connection);
            writes.add(equipment.getEquipmentId());
        }
    }

    private static class Tx {
        boolean autoCommit = true;
        final Connection connection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getAutoCommit" -> autoCommit;
                    case "setAutoCommit" -> { autoCommit = (boolean) arguments[0]; yield null; }
                    case "commit", "rollback", "close" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
