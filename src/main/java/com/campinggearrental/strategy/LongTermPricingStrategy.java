package com.campinggearrental.strategy;

import java.math.BigDecimal;
import java.util.Objects;

public class LongTermPricingStrategy implements PricingStrategy {
    private static final long MINIMUM_RENTAL_DAYS = 5;
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal, long rentalDays) {
        Objects.requireNonNull(subtotal, "subtotal must not be null");
        return rentalDays >= MINIMUM_RENTAL_DAYS
                ? subtotal.multiply(DISCOUNT_RATE)
                : BigDecimal.ZERO;
    }
}
