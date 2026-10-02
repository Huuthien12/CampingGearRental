package com.campinggearrental.service;

import static org.junit.jupiter.api.Assertions.*;

import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalDetail;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.repository.RentalOrderRepository;
import com.campinggearrental.state.PendingState;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PersistedCheckoutServiceTest {
    @Test void refreshesAndPersistsSnapshotBasedLongTermTotalsWithoutStateOrInventoryChanges() throws Exception {
        Store store = new Store(order(PaymentStatus.UNPAID, "100.00", 1, "30.00", "2"));
        Equipment catalog = new Equipment("EQ001", "Tent", "CAT", new BigDecimal("150.00"), 5, 3, null);
        PersistedCheckoutService service = service(store);

        RentalOrder refreshed = service.refreshPricing(" RENT001 ");

        assertSame(store.order, refreshed); assertEquals(new BigDecimal("800.00"), refreshed.getSubtotal());
        assertEquals(new BigDecimal("80.0000"), refreshed.getDiscount()); assertEquals(new BigDecimal("720.0000"), refreshed.getTotal());
        assertEquals(PaymentStatus.UNPAID, refreshed.getPaymentStatus()); assertInstanceOf(PendingState.class, refreshed.getCurrentState());
        assertEquals(3, catalog.getAvailableQuantity()); assertEquals(new BigDecimal("100.00"), refreshed.getDetails().getFirst().getUnitPrice());
        assertEquals(1, store.updates);
    }

    @Test void confirmsPaymentAfterRefreshingAndPersistsPaidWithoutChangingStateDetailsOrInventory() throws Exception {
        Store store = new Store(order(PaymentStatus.UNPAID, "100.00", 2));
        Equipment catalog = new Equipment("EQ001", "Tent", "CAT", new BigDecimal("150.00"), 5, 3, null);
        PersistedCheckoutService service = service(store);

        service.confirmPayment("RENT001");

        assertEquals(PaymentStatus.PAID, store.order.getPaymentStatus()); assertEquals(new BigDecimal("1000.00"), store.order.getSubtotal());
        assertEquals(new BigDecimal("100.0000"), store.order.getDiscount()); assertEquals(new BigDecimal("900.0000"), store.order.getTotal());
        assertInstanceOf(PendingState.class, store.order.getCurrentState()); assertEquals(3, catalog.getAvailableQuantity());
        assertEquals(new BigDecimal("100.00"), store.order.getDetails().getFirst().getUnitPrice()); assertEquals(2, store.order.getDetails().getFirst().getQuantity());
        assertEquals(1, store.updates);
    }

    @Test void rejectsMissingBlankUnknownAndAlreadyPaidOrdersWithoutChangingTotals() throws Exception {
        Store store = new Store(order(PaymentStatus.PAID, "100.00", 1));
        PersistedCheckoutService service = service(store);
        BigDecimal subtotal = store.order.getSubtotal();

        assertThrows(IllegalArgumentException.class, () -> service.load(null));
        assertThrows(IllegalArgumentException.class, () -> service.load(" "));
        assertThrows(IllegalArgumentException.class, () -> service.load("UNKNOWN"));
        assertThrows(IllegalStateException.class, () -> service.confirmPayment("RENT001"));
        assertEquals(subtotal, store.order.getSubtotal()); assertEquals(PaymentStatus.PAID, store.order.getPaymentStatus()); assertEquals(0, store.updates);

        Store missingStatus = new Store(order(null, "100.00", 1));
        assertThrows(IllegalStateException.class, () -> service(missingStatus).confirmPayment("RENT001"));
        assertEquals(0, missingStatus.updates);
    }

    @Test void restoresTotalsAndPaymentWhenPersistenceFails() {
        Store store = new Store(order(PaymentStatus.UNPAID, "100.00", 1)); store.failUpdate = true;
        PersistedCheckoutService service = service(store);
        BigDecimal subtotal = store.order.getSubtotal(); BigDecimal discount = store.order.getDiscount(); BigDecimal total = store.order.getTotal();

        assertThrows(SQLException.class, () -> service.refreshPricing("RENT001"));
        assertEquals(subtotal, store.order.getSubtotal()); assertEquals(discount, store.order.getDiscount()); assertEquals(total, store.order.getTotal());
        assertThrows(SQLException.class, () -> service.confirmPayment("RENT001"));
        assertEquals(PaymentStatus.UNPAID, store.order.getPaymentStatus());
    }

    private static PersistedCheckoutService service(Store store) {
        return new PersistedCheckoutService(store, new CheckoutService(new RentalPricingAdapter(new PricingService())));
    }

    private static RentalOrder order(PaymentStatus paymentStatus, String price, int quantity, String... extra) {
        RentalOrder order = new RentalOrder(); order.setId("RENT001"); order.setCustomerId("CUS001");
        order.setRentalDate(LocalDate.of(2026, 10, 1)); order.setExpectedReturnDate(LocalDate.of(2026, 10, 6));
        RentalDetail first = detail("EQ001", price, quantity);
        order.setDetails(extra.length == 0 ? List.of(first) : List.of(first, detail("EQ002", extra[0], Integer.parseInt(extra[1]))));
        order.setPaymentStatus(paymentStatus); order.setCurrentState(new PendingState());
        order.setSubtotal(BigDecimal.ONE); order.setDiscount(BigDecimal.ZERO); order.setTotal(BigDecimal.ONE);
        return order;
    }

    private static RentalDetail detail(String equipmentId, String price, int quantity) {
        RentalDetail detail = new RentalDetail(); detail.setEquipmentId(equipmentId); detail.setUnitPrice(new BigDecimal(price)); detail.setQuantity(quantity); return detail;
    }

    private static class Store implements RentalOrderRepository {
        final RentalOrder order; boolean failUpdate; int updates;
        Store(RentalOrder order) { this.order = order; }
        public void insert(RentalOrder rentalOrder) { }
        public Optional<RentalOrder> findById(String id) { return "RENT001".equals(id) ? Optional.of(order) : Optional.empty(); }
        public List<RentalOrder> findAll() { return List.of(order); }
        public void update(RentalOrder rentalOrder) throws SQLException { updates++; if (failUpdate) throw new SQLException("persistence failed"); }
    }
}
