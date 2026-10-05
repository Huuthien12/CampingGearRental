package com.campinggearrental.service;

import static org.junit.jupiter.api.Assertions.*;

import com.campinggearrental.factory.CampingPackageType;
import com.campinggearrental.model.*;
import com.campinggearrental.repository.*;
import com.campinggearrental.state.PendingState;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CampingPackageDraftServiceTest {
    private static final LocalDate START = LocalDate.of(2026, 10, 5);
    private static final LocalDate END = START.plusDays(2);

    @ParameterizedTest
    @EnumSource(CampingPackageType.class)
    void mapsPackageInOrderAndReturnsExistingServiceResult(CampingPackageType type) throws Exception {
        RecordingRentalService rental = new RecordingRentalService();
        var helper = new CampingPackageDraftService(rental);
        assertSame(rental.result, helper.createDraftFromPackage("CUS001", START, END, type));
        assertEquals(1, rental.calls);
        assertEquals(new RentalService.RentalRequest("CUS001", START, END, expected(type)), rental.request);
    }

    @ParameterizedTest
    @EnumSource(CampingPackageType.class)
    void createsRealPendingDraftWithSnapshotsWithoutChangingAnyStock(CampingPackageType type) throws Exception {
        Fixture f = fixture();
        Map<String, Integer> before = stock(f);
        RentalOrder order = f.helper.createDraftFromPackage("CUS001", START, END, type);
        assertSame(order, f.orders.saved);
        assertEquals(1, f.orders.insertCalls);
        assertInstanceOf(PendingState.class, order.getCurrentState());
        assertEquals(PaymentStatus.UNPAID, order.getPaymentStatus());
        assertNull(order.getActualReturnDate());
        assertEquals(BigDecimal.ZERO, order.getSubtotal());
        assertEquals(BigDecimal.ZERO, order.getDiscount());
        assertEquals(BigDecimal.ZERO, order.getTotal());
        assertEquals("CUS001", order.getCustomerId());
        assertEquals(START, order.getRentalDate());
        assertEquals(END, order.getExpectedReturnDate());
        assertEquals(expected(type), order.getDetails().stream()
                .map(d -> new RentalService.RentalRequestItem(d.getEquipmentId(), d.getQuantity())).toList());
        for (RentalDetail detail : order.getDetails()) {
            Equipment equipment = f.equipment.values.get(detail.getEquipmentId());
            BigDecimal snapshot = equipment.getPricePerDay();
            assertEquals(snapshot, detail.getUnitPrice());
            equipment.setPricePerDay(snapshot.add(BigDecimal.ONE));
            assertEquals(snapshot, detail.getUnitPrice());
        }
        assertEquals(before, stock(f));
        assertEquals(0, f.equipment.writeCalls);
    }

    @Test
    void rejectsNullTypeBeforeCallingRentalService() {
        RecordingRentalService rental = new RecordingRentalService();
        var helper = new CampingPackageDraftService(rental);
        var error = assertThrows(IllegalArgumentException.class,
                () -> helper.createDraftFromPackage("CUS001", START, END, null));
        assertEquals("package type is required", error.getMessage());
        assertEquals(0, rental.calls);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"CUS404"})
    void delegatesInvalidCustomerToExistingValidation(String customerId) {
        Fixture f = fixture();
        Map<String, Integer> before = stock(f);
        var error = assertThrows(IllegalArgumentException.class,
                () -> f.helper.createDraftFromPackage(customerId, START, END, CampingPackageType.SOLO));
        assertTrue(error.getMessage().contains("customer"));
        assertUnchanged(f, before);
    }

    @Test
    void delegatesMissingRentalDateToExistingValidation() {
        assertInvalidDates(null, END);
    }

    @Test
    void delegatesMissingReturnDateToExistingValidation() {
        assertInvalidDates(START, null);
    }

    @Test
    void delegatesReversedDatesToExistingValidation() {
        assertInvalidDates(START, START.minusDays(1));
    }

    @Test
    void rejectsMissingEquipmentAfterEarlierItemsWithoutPartialMutation() {
        Fixture f = fixture();
        f.equipment.values.remove("EQ005");
        assertEquipmentFailure(f, "equipment not found: EQ005");
    }

    @Test
    void rejectsInactiveEquipmentThroughExistingValidation() {
        Fixture f = fixture();
        f.equipment.values.get("EQ005").setStatus(EquipmentStatus.INACTIVE);
        assertEquipmentFailure(f, "equipment is inactive: EQ005");
    }

    @Test
    void rejectsInsufficientStockAfterEarlierItemsWithoutPartialMutation() {
        Fixture f = fixture();
        f.equipment.values.put("EQ005", new Equipment("EQ005", "Stove", "CAT005",
                new BigDecimal("50000"), 10, 0, EquipmentStatus.AVAILABLE));
        assertEquipmentFailure(f, "insufficient available equipment: EQ005");
    }

    @Test
    void propagatesPersistenceFailureWithoutChangingStock() {
        Fixture f = fixture();
        SQLException failure = new SQLException("test persistence failure");
        f.orders.failure = failure;
        Map<String, Integer> before = stock(f);
        assertSame(failure, assertThrows(SQLException.class,
                () -> f.helper.createDraftFromPackage("CUS001", START, END, CampingPackageType.FAMILY)));
        assertEquals(1, f.orders.insertCalls);
        assertNull(f.orders.saved);
        assertEquals(before, stock(f));
        assertEquals(0, f.equipment.writeCalls);
    }

    private static void assertInvalidDates(LocalDate start, LocalDate end) {
        Fixture f = fixture();
        Map<String, Integer> before = stock(f);
        var error = assertThrows(IllegalArgumentException.class,
                () -> f.helper.createDraftFromPackage("CUS001", start, end, CampingPackageType.SOLO));
        assertTrue(error.getMessage().contains("date"));
        assertUnchanged(f, before);
    }

    private static void assertEquipmentFailure(Fixture f, String message) {
        Map<String, Integer> before = stock(f);
        var error = assertThrows(IllegalArgumentException.class,
                () -> f.helper.createDraftFromPackage("CUS001", START, END, CampingPackageType.FAMILY));
        assertEquals(message, error.getMessage());
        assertUnchanged(f, before);
    }

    private static void assertUnchanged(Fixture f, Map<String, Integer> before) {
        assertEquals(0, f.orders.insertCalls);
        assertNull(f.orders.saved);
        assertEquals(before, stock(f));
        assertEquals(0, f.equipment.writeCalls);
    }

    private static Map<String, Integer> stock(Fixture f) {
        Map<String, Integer> stock = new LinkedHashMap<>();
        f.equipment.values.forEach((id, equipment) -> stock.put(id, equipment.getAvailableQuantity()));
        return stock;
    }

    private static List<RentalService.RentalRequestItem> expected(CampingPackageType type) {
        return switch (type) {
            case SOLO -> List.of(item("EQ001", 1), item("EQ002", 1), item("EQ004", 1));
            case COUPLE -> List.of(item("EQ001", 1), item("EQ002", 2), item("EQ003", 2), item("EQ004", 1));
            case FAMILY -> List.of(item("EQ001", 1), item("EQ002", 4), item("EQ003", 4),
                    item("EQ004", 2), item("EQ005", 1));
        };
    }

    private static RentalService.RentalRequestItem item(String id, int quantity) {
        return new RentalService.RentalRequestItem(id, quantity);
    }

    private static Fixture fixture() {
        EquipmentStore equipment = new EquipmentStore();
        for (int i = 1; i <= 5; i++) {
            String id = "EQ00" + i;
            equipment.values.put(id, new Equipment(id, "Equipment " + i, "CAT00" + i,
                    BigDecimal.valueOf(i * 10000L), 10, 8, EquipmentStatus.AVAILABLE));
        }
        Orders orders = new Orders();
        return new Fixture(equipment, orders, new CampingPackageDraftService(
                new RentalService(new Customers(), equipment, orders)));
    }

    private record Fixture(EquipmentStore equipment, Orders orders, CampingPackageDraftService helper) { }

    private static class RecordingRentalService extends RentalService {
        final RentalOrder result = new RentalOrder();
        RentalRequest request;
        int calls;
        RecordingRentalService() { super(new Customers(), new EquipmentStore(), new Orders()); }
        @Override public RentalOrder createDraft(RentalRequest request) {
            this.request = request;
            calls++;
            return result;
        }
    }

    private static class Customers implements CustomerRepository {
        public List<Customer> findAll() { return List.of(new Customer("CUS001", "Customer", "0900", "", "")); }
        public List<Customer> search(String keyword) { return List.of(); }
        public Optional<Customer> findByPhone(String phone) { return Optional.empty(); }
        public void insert(Customer customer) { throw new AssertionError("Unexpected customer write"); }
        public void update(Customer customer) { throw new AssertionError("Unexpected customer write"); }
        public void deleteById(String id) { throw new AssertionError("Unexpected customer write"); }
    }

    private static class EquipmentStore implements EquipmentRepository {
        final Map<String, Equipment> values = new LinkedHashMap<>();
        int writeCalls;
        public List<Equipment> findAll() { return List.copyOf(values.values()); }
        public Optional<Equipment> findById(String id) { return Optional.ofNullable(values.get(id)); }
        public List<Equipment> search(String keyword) { return List.of(); }
        public void insert(Equipment equipment) { writeCalls++; throw new AssertionError("Unexpected inventory write"); }
        public void update(Equipment equipment) { writeCalls++; throw new AssertionError("Unexpected inventory write"); }
    }

    private static class Orders implements RentalOrderRepository {
        RentalOrder saved;
        SQLException failure;
        int insertCalls;
        public void insert(RentalOrder order) throws SQLException {
            insertCalls++;
            if (failure != null) throw failure;
            saved = order;
        }
        public Optional<RentalOrder> findById(String id) { return Optional.ofNullable(saved); }
        public List<RentalOrder> findAll() { return saved == null ? List.of() : List.of(saved); }
        public void update(RentalOrder order) { throw new AssertionError("Unexpected order update"); }
    }
}
