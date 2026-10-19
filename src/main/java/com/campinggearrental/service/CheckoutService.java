package com.campinggearrental.service;

import com.campinggearrental.model.PaymentMethod;
import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalOrder;
import java.util.Objects;
import java.time.LocalDateTime;

public class CheckoutService {
    private final RentalPricingAdapter rentalPricingAdapter;

    public CheckoutService(RentalPricingAdapter rentalPricingAdapter) {
        this.rentalPricingAdapter = Objects.requireNonNull(rentalPricingAdapter, "rentalPricingAdapter must not be null");
    }

    public PricingService.PricingResult refresh(RentalOrder rentalOrder) {
        return rentalPricingAdapter.calculateAndApply(rentalOrder);
    }

    public void confirmPayment(RentalOrder rentalOrder) {
        Objects.requireNonNull(rentalOrder, "rentalOrder must not be null");
        requireUnpaid(rentalOrder);
        if (rentalOrder.getPaymentMethod() != null && rentalOrder.getPaymentMethod() != PaymentMethod.CASH
                && rentalOrder.getPaymentStatus() != PaymentStatus.TRANSFER_REPORTED) {
            throw new IllegalStateException("transfer must be reported before payment confirmation");
        }
        rentalOrder.setPaymentStatus(PaymentStatus.PAID);
        rentalOrder.setPaidAt(LocalDateTime.now());
    }

    public PricingService.PricingResult refreshAndConfirmPayment(RentalOrder rentalOrder) {
        Objects.requireNonNull(rentalOrder, "rentalOrder must not be null");
        requireUnpaid(rentalOrder);
        PricingService.PricingResult result = refresh(rentalOrder);
        confirmPayment(rentalOrder);
        return result;
    }

    public void selectPaymentMethod(RentalOrder order, PaymentMethod method) {
        Objects.requireNonNull(order, "rentalOrder must not be null");
        Objects.requireNonNull(method, "payment method must not be null");
        if (method != PaymentMethod.CASH && method != PaymentMethod.BANK_TRANSFER) {
            throw new IllegalArgumentException("payment method is no longer available");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID || order.getPaymentStatus() == PaymentStatus.TRANSFER_REPORTED) {
            throw new IllegalStateException("payment method cannot be changed now");
        }
        order.setPaymentMethod(method);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setPaidAt(null);
    }

    public void reportTransfer(RentalOrder order) {
        Objects.requireNonNull(order, "rentalOrder must not be null");
        if (!isBankTransfer(order.getPaymentMethod())) {
            throw new IllegalStateException("only bank transfers can be reported");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID || order.getPaymentStatus() == PaymentStatus.TRANSFER_REPORTED) {
            throw new IllegalStateException("transfer cannot be reported now");
        }
        order.setPaymentStatus(PaymentStatus.TRANSFER_REPORTED);
    }

    public void requestReview(RentalOrder order) { setTransferReviewStatus(order, PaymentStatus.REVIEW_REQUIRED); }

    public void rejectTransfer(RentalOrder order) { setTransferReviewStatus(order, PaymentStatus.REJECTED); }

    private void setTransferReviewStatus(RentalOrder order, PaymentStatus status) {
        Objects.requireNonNull(order, "rentalOrder must not be null");
        if (order.getPaymentStatus() != PaymentStatus.TRANSFER_REPORTED) {
            throw new IllegalStateException("only reported transfers can be reviewed");
        }
        order.setPaymentStatus(status);
    }

    private static void requireUnpaid(RentalOrder rentalOrder) {
        if (rentalOrder.getPaymentStatus() == null || rentalOrder.getPaymentStatus() == PaymentStatus.PAID
                || rentalOrder.getPaymentStatus() == PaymentStatus.REJECTED
                || rentalOrder.getPaymentStatus() == PaymentStatus.REVIEW_REQUIRED) {
            throw new IllegalStateException("payment cannot be confirmed now");
        }
    }

    private static boolean isBankTransfer(PaymentMethod method) {
        return method == PaymentMethod.BANK_TRANSFER || method == PaymentMethod.VIETQR;
    }
}
