package com.campinggearrental.strategy;

import java.math.BigDecimal;

public interface PricingStrategy {
    BigDecimal calculateDiscount(BigDecimal subtotal, long rentalDays);
}
