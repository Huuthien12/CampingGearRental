package com.campinggearrental.service;

import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalOrder;
import java.util.Objects;

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
        rentalOrder.setPaymentStatus(PaymentStatus.PAID);
    }

    public PricingService.PricingResult refreshAndConfirmPayment(RentalOrder rentalOrder) {
        Objects.requireNonNull(rentalOrder, "rentalOrder must not be null");
        requireUnpaid(rentalOrder);
        PricingService.PricingResult result = refresh(rentalOrder);
        rentalOrder.setPaymentStatus(PaymentStatus.PAID);
        return result;
    }

    private static void requireUnpaid(RentalOrder rentalOrder) {
        if (rentalOrder.getPaymentStatus() != PaymentStatus.UNPAID) {
            throw new IllegalStateException("only unpaid orders can be paid");
        }
    }
}
