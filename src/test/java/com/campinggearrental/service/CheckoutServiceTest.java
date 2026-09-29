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

class CheckoutServiceTest {
    private final CheckoutService checkoutService = new CheckoutService(
            new RentalPricingAdapter(new PricingService()));

    @Test
    void refreshesAnUnpaidOrderAndWritesPricingTotals() {
        RentalOrder order = order(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 25),
                detail("100000.00", 1));
        order.setPaymentStatus(PaymentStatus.UNPAID);

        checkoutService.refresh(order);

        assertEquals(new BigDecimal("500000.00"), order.getSubtotal());
        assertEquals(new BigDecimal("50000.0000"), order.getDiscount());
        assertEquals(new BigDecimal("450000.0000"), order.getTotal());
        assertEquals(PaymentStatus.UNPAID, order.getPaymentStatus());
    }

    @Test
    void confirmsPaymentWithoutChangingTotalsOrDetailSnapshot() {
        RentalDetail detail = detail("100000.00", 2);
        RentalOrder order = order(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 23), detail);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        checkoutService.refresh(order);
        BigDecimal subtotal = order.getSubtotal();
        BigDecimal discount = order.getDiscount();
        BigDecimal total = order.getTotal();

        checkoutService.confirmPayment(order);

        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(subtotal, order.getSubtotal());
        assertEquals(discount, order.getDiscount());
        assertEquals(total, order.getTotal());
        assertEquals(new BigDecimal("100000.00"), detail.getUnitPrice());
        assertEquals(2, detail.getQuantity());
    }

    @Test
    void rejectsMissingOrAlreadyPaidPaymentStatus() {
        RentalOrder missingStatus = order(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21),
                detail("100000.00", 1));
        RentalOrder paidOrder = order(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21),
                detail("100000.00", 1));
        paidOrder.setPaymentStatus(PaymentStatus.PAID);

        assertThrows(IllegalStateException.class, () -> checkoutService.confirmPayment(missingStatus));
        assertThrows(IllegalStateException.class, () -> checkoutService.confirmPayment(paidOrder));
    }

    @Test
    void passesInvalidPricingInputToExistingValidationWithoutEquipmentLookup() {
        RentalOrder order = order(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 19),
                detail("100000.00", 1));

        assertThrows(IllegalArgumentException.class, () -> checkoutService.refresh(order));
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
