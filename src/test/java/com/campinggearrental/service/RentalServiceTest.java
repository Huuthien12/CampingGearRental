package com.campinggearrental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campinggearrental.model.Customer;
import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.repository.CustomerRepository;
import com.campinggearrental.repository.EquipmentRepository;
import com.campinggearrental.repository.RentalOrderRepository;
import com.campinggearrental.state.PendingState;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RentalServiceTest {
    @Test
    void createsAndPersistsPendingDraftWithPriceSnapshotsWithoutMutatingStock() throws Exception {
        Fixture fixture = fixture();
        int tentAvailable = fixture.tent.getAvailableQuantity();
        RentalOrder order = fixture.service.createDraft(request(items("EQ001", 1, "EQ002", 2)));

        assertEquals(1, fixture.orders.values.size());
        assertTrue(order.getId().matches("RENT[0-9a-f]{16}"));
        assertInstanceOf(PendingState.class, order.getCurrentState());
        assertEquals(PaymentStatus.UNPAID, order.getPaymentStatus());
        assertNull(order.getActualReturnDate());
        assertEquals(BigDecimal.ZERO, order.getSubtotal());
        assertEquals(BigDecimal.ZERO, order.getDiscount());
        assertEquals(BigDecimal.ZERO, order.getTotal());
        assertEquals(2, order.getDetails().size());
        assertEquals(new BigDecimal("100000.00"), order.getDetails().getFirst().getUnitPrice());
        fixture.tent.setPricePerDay(new BigDecimal("200000.00"));
        assertEquals(new BigDecimal("100000.00"), order.getDetails().getFirst().getUnitPrice());
        assertEquals(tentAvailable, fixture.tent.getAvailableQuantity());
    }

    @Test
    void rejectsInvalidCustomerDatesAndItemsWithoutPersisting() {
        Fixture fixture = fixture();
        assertInvalid(fixture, new RentalService.RentalRequest("", LocalDate.now(), LocalDate.now(), items("EQ001", 1)));
        assertInvalid(fixture, new RentalService.RentalRequest("CUS404", LocalDate.now(), LocalDate.now(), items("EQ001", 1)));
        assertInvalid(fixture, new RentalService.RentalRequest("CUS001", null, LocalDate.now(), items("EQ001", 1)));
        assertInvalid(fixture, new RentalService.RentalRequest("CUS001", LocalDate.now(), null, items("EQ001", 1)));
        assertInvalid(fixture, new RentalService.RentalRequest("CUS001", LocalDate.now(), LocalDate.now().minusDays(1), items("EQ001", 1)));
        assertInvalid(fixture, new RentalService.RentalRequest("CUS001", LocalDate.now(), LocalDate.now(), List.of()));
        assertInvalid(fixture, new RentalService.RentalRequest("CUS001", LocalDate.now(), LocalDate.now(), items(" ", 1)));
        assertInvalid(fixture, new RentalService.RentalRequest("CUS001", LocalDate.now(), LocalDate.now(), items("EQ001", 0)));
        assertEquals(0, fixture.orders.insertCalls);
    }

    @Test
    void rejectsMissingInactiveAndInsufficientEquipmentWithoutPersisting() {
        Fixture fixture = fixture();
        assertInvalid(fixture, request(items("EQ404", 1)));
        fixture.tent.setStatus(EquipmentStatus.INACTIVE);
        assertInvalid(fixture, request(items("EQ001", 1)));
        fixture.tent.setStatus(EquipmentStatus.AVAILABLE);
        assertInvalid(fixture, request(items("EQ001", fixture.tent.getAvailableQuantity() + 1)));
        assertEquals(0, fixture.orders.insertCalls);
    }

    @Test
    void aggregatesDuplicateEquipmentIdsBeforeCheckingAndPersisting() throws Exception {
        Fixture fixture = fixture();
        RentalOrder order = fixture.service.createDraft(request(items("EQ001", 1, "EQ001", 2)));

        assertEquals(1, order.getDetails().size());
        assertEquals(3, order.getDetails().getFirst().getQuantity());
        assertEquals(5, fixture.tent.getAvailableQuantity());
    }

    private static void assertInvalid(Fixture fixture, RentalService.RentalRequest request) {
        assertThrows(IllegalArgumentException.class, () -> fixture.service.createDraft(request));
        assertTrue(fixture.orders.values.isEmpty());
    }

    private static RentalService.RentalRequest request(List<RentalService.RentalRequestItem> items) {
        return new RentalService.RentalRequest("CUS001", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), items);
    }

    private static List<RentalService.RentalRequestItem> items(Object... values) {
        List<RentalService.RentalRequestItem> items = new ArrayList<>();
        for (int index = 0; index < values.length; index += 2) {
            items.add(new RentalService.RentalRequestItem((String) values[index], (Integer) values[index + 1]));
        }
        return items;
    }

    private static Fixture fixture() {
        Equipment tent = new Equipment("EQ001", "Tent", "CAT001", new BigDecimal("100000.00"), 5);
        Equipment bag = new Equipment("EQ002", "Bag", "CAT002", new BigDecimal("40000.00"), 10);
        MemoryOrders orders = new MemoryOrders();
        return new Fixture(tent, orders, new RentalService(new Customers(), new EquipmentStore(tent, bag), orders));
    }

    private record Fixture(Equipment tent, MemoryOrders orders, RentalService service) { }

    private static class Customers implements CustomerRepository {
        public List<Customer> findAll() { return List.of(new Customer("CUS001", "Name", "0900", "", "")); }
        public List<Customer> search(String keyword) { return List.of(); }
        public Optional<Customer> findByPhone(String phone) { return Optional.empty(); }
        public void insert(Customer customer) { }
        public void update(Customer customer) { }
        public void deleteById(String id) { }
    }

    private static class EquipmentStore implements EquipmentRepository {
        private final List<Equipment> values;
        EquipmentStore(Equipment... values) { this.values = List.of(values); }
        public List<Equipment> findAll() { return values; }
        public Optional<Equipment> findById(String id) { return values.stream().filter(e -> e.getEquipmentId().equals(id)).findFirst(); }
        public List<Equipment> search(String keyword) { return List.of(); }
        public void insert(Equipment equipment) { }
        public void update(Equipment equipment) { }
    }

    private static class MemoryOrders implements RentalOrderRepository {
        private final List<RentalOrder> values = new ArrayList<>();
        private int insertCalls;
        public void insert(RentalOrder order) { values.add(order); insertCalls++; }
        public Optional<RentalOrder> findById(String id) { return values.stream().filter(o -> o.getId().equals(id)).findFirst(); }
        public List<RentalOrder> findAll() { return List.copyOf(values); }
        public void update(RentalOrder order) { }
    }
}
