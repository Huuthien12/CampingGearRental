package com.campinggearrental.strategy;

import java.math.BigDecimal;

public class NormalPricingStrategy implements PricingStrategy {
    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal, long rentalDays) {
        return BigDecimal.ZERO;
    }
}
