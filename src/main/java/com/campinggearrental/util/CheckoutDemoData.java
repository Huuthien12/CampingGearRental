package com.campinggearrental.util;

import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalDetail;
import com.campinggearrental.model.RentalOrder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Temporary data used only to launch the standalone checkout screen. */
public final class CheckoutDemoData {
    private CheckoutDemoData() {
    }

    public static RentalOrder createOrder() {
        RentalOrder order = new RentalOrder();
        order.setRentalDate(LocalDate.of(2026, 10, 20));
        order.setExpectedReturnDate(LocalDate.of(2026, 10, 25));
        order.setDetails(List.of(detail("100000.00", 1), detail("30000.00", 2)));
        order.setPaymentStatus(PaymentStatus.UNPAID);
        return order;
    }

    private static RentalDetail detail(String unitPrice, int quantity) {
        RentalDetail detail = new RentalDetail();
        detail.setUnitPrice(new BigDecimal(unitPrice));
        detail.setQuantity(quantity);
        return detail;
    }
}
