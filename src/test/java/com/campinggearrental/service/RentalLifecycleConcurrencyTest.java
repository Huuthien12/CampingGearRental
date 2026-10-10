package com.campinggearrental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalDetail;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.repository.CustomerRepository;
import com.campinggearrental.repository.EquipmentRepository;
import com.campinggearrental.repository.RentalOrderRepository;
import com.campinggearrental.state.CancelledState;
import com.campinggearrental.state.ConfirmedState;
import com.campinggearrental.state.PendingState;
import com.campinggearrental.state.RentalState;
import com.campinggearrental.state.ReturnedState;
import com.campinggearrental.state.RentedState;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;

class RentalLifecycleConcurrencyTest {
    @Test void competingOrdersReserveStockAtMostOnce() throws Exception {
        Fixture fixture = fixture(5);
        fixture.orders.put(order("A", new PendingState(), 3));
        fixture.orders.put(order("B", new PendingState(), 4));

        List<Boolean> results = concurrently(
                () -> confirm(fixture.service, "A"),
                () -> confirm(fixture.service, "B"));

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals(1, fixture.orders.all().stream().filter(order -> order.getCurrentState() instanceof ConfirmedState).count());
        assertEquals(1, fixture.orders.all().stream().filter(order -> order.getCurrentState() instanceof PendingState).count());
        assertEquals(true, fixture.equipment.getAvailableQuantity() == 2 || fixture.equipment.getAvailableQuantity() == 1);
        assertEquals(true, fixture.equipment.getAvailableQuantity() >= 0);
    }

    @Test void doubleConfirmationOfOneOrderReservesOnlyOnce() throws Exception {
        Fixture fixture = fixture(5);
        fixture.orders.put(order("A", new PendingState(), 3));

        List<Boolean> results = concurrently(
                () -> confirm(fixture.service, "A"),
                () -> confirm(fixture.service, "A"));

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertInstanceOf(ConfirmedState.class, fixture.orders.get("A").getCurrentState());
        assertEquals(2, fixture.equipment.getAvailableQuantity());
    }

    @Test void confirmationAndCancellationProduceAValidSerializedResult() throws Exception {
        Fixture fixture = fixture(5);
        fixture.orders.put(order("A", new PendingState(), 3));

        List<Boolean> results = concurrently(
                () -> confirm(fixture.service, "A"),
                () -> cancel(fixture.service, "A"));

        assertEquals(2, results.size());
        assertInstanceOf(CancelledState.class, fixture.orders.get("A").getCurrentState());
        assertEquals(5, fixture.equipment.getAvailableQuantity());
    }

    @Test void concurrentReturnsReleaseReservedStockOnlyOnce() throws Exception {
        Fixture fixture = fixture(2);
        fixture.orders.put(order("A", new RentedState(), 3));

        List<Boolean> results = concurrently(
                () -> returnRental(fixture.service, "A"),
                () -> returnRental(fixture.service, "A"));

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertInstanceOf(ReturnedState.class, fixture.orders.get("A").getCurrentState());
        assertEquals(5, fixture.equipment.getAvailableQuantity());
    }

    private static boolean confirm(RentalService service, String id) throws Exception {
        try { service.confirmRental(id); return true; }
        catch (IllegalStateException ignored) { return false; }
    }

    private static boolean cancel(RentalService service, String id) throws Exception {
        try { service.cancelRental(id); return true; }
        catch (IllegalStateException ignored) { return false; }
    }

    private static boolean returnRental(RentalService service, String id) throws Exception {
        try { service.returnRental(id, LocalDate.of(2026, 10, 5)); return true; }
        catch (IllegalStateException ignored) { return false; }
    }

    private static List<Boolean> concurrently(Callable<Boolean> first, Callable<Boolean> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> one = executor.submit(gated(first, ready, start));
            Future<Boolean> two = executor.submit(gated(second, ready, start));
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            return List.of(one.get(5, TimeUnit.SECONDS), two.get(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private static Callable<Boolean> gated(Callable<Boolean> action, CountDownLatch ready, CountDownLatch start) {
        return () -> { ready.countDown(); start.await(5, TimeUnit.SECONDS); return action.call(); };
    }

    private static Fixture fixture(int available) {
        LockingTransactions transactions = new LockingTransactions();
        LockingOrders orders = new LockingOrders(transactions);
        LockingEquipment equipmentRepository = new LockingEquipment(transactions);
        Equipment equipment = new Equipment("EQ001", "Tent", "CAT", BigDecimal.TEN, 5, available, EquipmentStatus.AVAILABLE);
        equipmentRepository.values.put(equipment.getEquipmentId(), equipment);
        RentalService service = new RentalService(new Customers(), equipmentRepository, orders, transactions::connection);
        return new Fixture(service, orders, equipment);
    }

    private static RentalOrder order(String id, RentalState state, int quantity) {
        RentalDetail detail = new RentalDetail();
        detail.setEquipmentId("EQ001"); detail.setQuantity(quantity); detail.setUnitPrice(BigDecimal.TEN);
        RentalOrder order = new RentalOrder();
        order.setId(id); order.setRentalDate(LocalDate.of(2026, 10, 1)); order.setExpectedReturnDate(LocalDate.of(2026, 10, 4));
        order.setDetails(List.of(detail)); order.setCurrentState(state); order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setSubtotal(BigDecimal.ZERO); order.setDiscount(BigDecimal.ZERO); order.setTotal(BigDecimal.ZERO);
        return order;
    }

    private record Fixture(RentalService service, LockingOrders orders, Equipment equipment) { }

    private static class Customers implements CustomerRepository {
        public List<com.campinggearrental.model.Customer> findAll() { return List.of(); }
        public Optional<com.campinggearrental.model.Customer> findById(String id) { return Optional.empty(); }
        public List<com.campinggearrental.model.Customer> search(String keyword) { return List.of(); }
        public Optional<com.campinggearrental.model.Customer> findByPhone(String phone) { return Optional.empty(); }
        public void insert(com.campinggearrental.model.Customer customer) { }
        public void update(com.campinggearrental.model.Customer customer) { }
        public void deleteById(String id) { }
    }

    private static class LockingOrders implements RentalOrderRepository {
        final Map<String, RentalOrder> values = new java.util.concurrent.ConcurrentHashMap<>();
        final Map<String, ReentrantLock> locks = new java.util.concurrent.ConcurrentHashMap<>();
        final LockingTransactions transactions;
        LockingOrders(LockingTransactions transactions) { this.transactions = transactions; }
        void put(RentalOrder order) { values.put(order.getId(), order); }
        RentalOrder get(String id) { return values.get(id); }
        List<RentalOrder> all() { return new ArrayList<>(values.values()); }
        public void insert(RentalOrder order) { put(order); }
        public Optional<RentalOrder> findById(String id) { return Optional.ofNullable(values.get(id)); }
        public Optional<RentalOrder> findByIdForUpdate(Connection connection, String id) {
            transactions.lock(connection, locks.computeIfAbsent(id, unused -> new ReentrantLock()));
            return findById(id);
        }
        public List<RentalOrder> findAll() { return new ArrayList<>(values.values()); }
        public void update(RentalOrder order) { put(order); }
        public void update(Connection connection, RentalOrder order) { put(order); }
    }

    private static class LockingEquipment implements EquipmentRepository {
        final Map<String, Equipment> values = new java.util.concurrent.ConcurrentHashMap<>();
        final Map<String, ReentrantLock> locks = new java.util.concurrent.ConcurrentHashMap<>();
        final LockingTransactions transactions;
        LockingEquipment(LockingTransactions transactions) { this.transactions = transactions; }
        public List<Equipment> findAll() { return new ArrayList<>(values.values()); }
        public Optional<Equipment> findById(String id) { return Optional.ofNullable(values.get(id)); }
        public Optional<Equipment> findByIdForUpdate(Connection connection, String id) {
            transactions.lock(connection, locks.computeIfAbsent(id, unused -> new ReentrantLock()));
            return findById(id);
        }
        public List<Equipment> search(String keyword) { return List.of(); }
        public void insert(Equipment equipment) { values.put(equipment.getEquipmentId(), equipment); }
        public void update(Equipment equipment) { values.put(equipment.getEquipmentId(), equipment); }
        public void update(Connection connection, Equipment equipment) { update(equipment); }
    }

    private static class LockingTransactions {
        private final Map<Connection, Transaction> transactions = Collections.synchronizedMap(new IdentityHashMap<>());

        Connection connection() {
            Transaction transaction = new Transaction();
            Connection connection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, arguments) -> switch (method.getName()) {
                        case "getAutoCommit" -> transaction.autoCommit;
                        case "setAutoCommit" -> { transaction.autoCommit = (boolean) arguments[0]; yield null; }
                        case "commit", "rollback", "close" -> { transaction.releaseLocks(); yield null; }
                        case "isClosed", "isWrapperFor" -> false;
                        default -> method.getReturnType() == boolean.class ? false : method.getReturnType() == int.class ? 0 : null;
                    });
            transactions.put(connection, transaction);
            return connection;
        }

        void lock(Connection connection, ReentrantLock lock) {
            Transaction transaction = transactions.get(connection);
            if (transaction == null) throw new IllegalArgumentException("unknown transaction connection");
            transaction.lock(lock);
        }

        private static class Transaction {
            boolean autoCommit = true;
            final List<ReentrantLock> locks = new ArrayList<>();
            void lock(ReentrantLock lock) { lock.lock(); locks.add(lock); }
            void releaseLocks() { for (int index = locks.size() - 1; index >= 0; index--) locks.get(index).unlock(); locks.clear(); }
        }
    }
}
