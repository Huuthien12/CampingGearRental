package com.campinggearrental.service;

import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.model.PaymentMethod;
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
        if (order.getPaymentStatus() == com.campinggearrental.model.PaymentStatus.PAID
                || order.getPaymentStatus() == com.campinggearrental.model.PaymentStatus.TRANSFER_REPORTED) {
            throw new IllegalStateException("pricing is locked for this payment status");
        }
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

    // shortcut: the demo runs one application instance; use a conditional SQL update before horizontal scaling.
    public synchronized RentalOrder confirmPayment(String rentalId) throws SQLException {
        RentalOrder order = load(rentalId);
        Snapshot snapshot = Snapshot.of(order);
        try {
            if (order.getPaymentStatus() == com.campinggearrental.model.PaymentStatus.TRANSFER_REPORTED) {
                checkoutService.confirmPayment(order);
            } else {
                checkoutService.refreshAndConfirmPayment(order);
            }
            rentalOrderRepository.update(order);
            return order;
        } catch (SQLException | RuntimeException exception) {
            snapshot.restore(order);
            throw exception;
        }
    }

    public RentalOrder selectPaymentMethod(String rentalId, PaymentMethod method) throws SQLException {
        return updatePayment(rentalId, order -> checkoutService.selectPaymentMethod(order, method));
    }

    public RentalOrder reportTransfer(String rentalId) throws SQLException {
        return updatePayment(rentalId, checkoutService::reportTransfer);
    }

    public RentalOrder requestReview(String rentalId) throws SQLException {
        return updatePayment(rentalId, checkoutService::requestReview);
    }

    public RentalOrder rejectTransfer(String rentalId) throws SQLException {
        return updatePayment(rentalId, checkoutService::rejectTransfer);
    }

    private RentalOrder updatePayment(String rentalId, java.util.function.Consumer<RentalOrder> change) throws SQLException {
        RentalOrder order = load(rentalId);
        Snapshot snapshot = Snapshot.of(order);
        try {
            change.accept(order);
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

    private record Snapshot(BigDecimal subtotal, BigDecimal discount, BigDecimal total, PaymentMethod paymentMethod,
                            com.campinggearrental.model.PaymentStatus paymentStatus, java.time.LocalDateTime paidAt) {
        static Snapshot of(RentalOrder order) {
            return new Snapshot(order.getSubtotal(), order.getDiscount(), order.getTotal(), order.getPaymentMethod(),
                    order.getPaymentStatus(), order.getPaidAt());
        }
        void restore(RentalOrder order) {
            order.setSubtotal(subtotal);
            order.setDiscount(discount);
            order.setTotal(total);
            order.setPaymentMethod(paymentMethod);
            order.setPaymentStatus(paymentStatus);
            order.setPaidAt(paidAt);
        }
    }
}
