package com.campinggearrental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalDetail;
import com.campinggearrental.model.RentalOrder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RentalPricingAdapterTest {
    private final RentalPricingAdapter adapter = new RentalPricingAdapter(new PricingService());

    @Test
    void appliesOneDetailSnapshotPriceToOrderTotals() {
        RentalOrder order = order(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 23),
                detail("125000.00", 2));

        adapter.calculateAndApply(order);

        assertEquals(new BigDecimal("750000.00"), order.getSubtotal());
        assertEquals(BigDecimal.ZERO, order.getDiscount());
        assertEquals(new BigDecimal("750000.00"), order.getTotal());
    }

    @Test
    void sumsMultipleDetailsWithoutEquipmentLookup() {
        RentalOrder order = order(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 23),
                detail("100000.00", 1), detail("30000.00", 2));

        adapter.calculateAndApply(order);

        assertEquals(new BigDecimal("480000.00"), order.getSubtotal());
    }

    @Test
    void usesMinimumOneDayForSameDayRental() {
        RentalOrder order = order(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 20),
                detail("100000.00", 1));

        adapter.calculateAndApply(order);

        assertEquals(new BigDecimal("100000.00"), order.getSubtotal());
    }

    @Test
    void appliesLongTermDiscountAndWritesAllTotals() {
        RentalOrder order = order(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 25),
                detail("100000.00", 1));

        adapter.calculateAndApply(order);

        assertEquals(new BigDecimal("500000.00"), order.getSubtotal());
        assertEquals(new BigDecimal("50000.0000"), order.getDiscount());
        assertEquals(new BigDecimal("450000.0000"), order.getTotal());
    }

    @Test
    void leavesPaymentStatusUnpaid() {
        RentalOrder order = order(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 25),
                detail("100000.00", 1));
        order.setPaymentStatus(PaymentStatus.UNPAID);

        adapter.calculateAndApply(order);

        assertEquals(PaymentStatus.UNPAID, order.getPaymentStatus());
    }

    @Test
    void rejectsReturnDateBeforeRentalDate() {
        RentalOrder order = order(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 19),
                detail("100000.00", 1));

        assertThrows(IllegalArgumentException.class, () -> adapter.calculateAndApply(order));
    }

    private RentalOrder order(LocalDate rentalDate, LocalDate expectedReturnDate, RentalDetail... details) {
        RentalOrder order = new RentalOrder();
        order.setRentalDate(rentalDate);
        order.setExpectedReturnDate(expectedReturnDate);
        order.setDetails(List.of(details));
        return order;
    }

    private RentalDetail detail(String unitPrice, int quantity) {
        RentalDetail detail = new RentalDetail();
        detail.setUnitPrice(new BigDecimal(unitPrice));
        detail.setQuantity(quantity);
        return detail;
    }
}
