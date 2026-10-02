package com.campinggearrental.service;

import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.repository.RentalOrderRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Objects;

/** Applies existing checkout rules to a real rental order and persists the aggregate. */
public class PersistedCheckoutService {
    private final RentalOrderRepository rentalOrderRepository;
    private final CheckoutService checkoutService;

    public PersistedCheckoutService(RentalOrderRepository rentalOrderRepository, CheckoutService checkoutService) {
        this.rentalOrderRepository = Objects.requireNonNull(rentalOrderRepository, "rentalOrderRepository must not be null");
        this.checkoutService = Objects.requireNonNull(checkoutService, "checkoutService must not be null");
    }

    public RentalOrder load(String rentalId) throws SQLException {
        String id = requireId(rentalId);
        return rentalOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("rental order not found: " + id));
    }

    public RentalOrder refreshPricing(String rentalId) throws SQLException {
        RentalOrder order = load(rentalId);
        Snapshot snapshot = Snapshot.of(order);
        try {
            checkoutService.refresh(order);
            rentalOrderRepository.update(order);
            return order;
        } catch (SQLException | RuntimeException exception) {
            snapshot.restore(order);
            throw exception;
        }
    }

    public RentalOrder confirmPayment(String rentalId) throws SQLException {
        RentalOrder order = load(rentalId);
        Snapshot snapshot = Snapshot.of(order);
        try {
            checkoutService.refreshAndConfirmPayment(order);
            rentalOrderRepository.update(order);
            return order;
        } catch (SQLException | RuntimeException exception) {
            snapshot.restore(order);
            throw exception;
        }
    }

    private static String requireId(String rentalId) {
        if (rentalId == null || rentalId.isBlank()) throw new IllegalArgumentException("rental order id must not be blank");
        return rentalId.trim();
    }

    private record Snapshot(BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                            com.campinggearrental.model.PaymentStatus paymentStatus) {
        static Snapshot of(RentalOrder order) {
            return new Snapshot(order.getSubtotal(), order.getDiscount(), order.getTotal(), order.getPaymentStatus());
        }
        void restore(RentalOrder order) {
            order.setSubtotal(subtotal);
            order.setDiscount(discount);
            order.setTotal(total);
            order.setPaymentStatus(paymentStatus);
        }
    }
}
