package com.campinggearrental.service;

import com.campinggearrental.strategy.LongTermPricingStrategy;
import com.campinggearrental.strategy.NormalPricingStrategy;
import com.campinggearrental.strategy.PricingStrategy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public class PricingService {
    private final PricingStrategy normalPricingStrategy = new NormalPricingStrategy();
    private final PricingStrategy longTermPricingStrategy = new LongTermPricingStrategy();

    public long calculateRentalDays(LocalDate rentalDate, LocalDate expectedReturnDate) {
        Objects.requireNonNull(rentalDate, "rentalDate must not be null");
        Objects.requireNonNull(expectedReturnDate, "expectedReturnDate must not be null");
        if (expectedReturnDate.isBefore(rentalDate)) {
            throw new IllegalArgumentException("expectedReturnDate must not be before rentalDate");
        }
        return Math.max(1, ChronoUnit.DAYS.between(rentalDate, expectedReturnDate));
    }

    public BigDecimal calculateLineTotal(BigDecimal unitPrice, int quantity, long rentalDays) {
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (unitPrice.signum() <= 0 || quantity <= 0 || rentalDays < 1) {
            throw new IllegalArgumentException("unitPrice, quantity, and rentalDays must be valid");
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).multiply(BigDecimal.valueOf(rentalDays));
    }

    public BigDecimal calculateSubtotal(List<PricingLine> lines, long rentalDays) {
        Objects.requireNonNull(lines, "lines must not be null");
        return lines.stream()
                .map(line -> calculateLineTotal(line.unitPrice(), line.quantity(), rentalDays))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public PricingResult calculate(List<PricingLine> lines, LocalDate rentalDate, LocalDate expectedReturnDate) {
        long rentalDays = calculateRentalDays(rentalDate, expectedReturnDate);
        BigDecimal subtotal = calculateSubtotal(lines, rentalDays);
        BigDecimal discount = selectStrategy(rentalDays).calculateDiscount(subtotal, rentalDays);
        return new PricingResult(rentalDays, subtotal, discount, subtotal.subtract(discount));
    }

    public PricingStrategy selectStrategy(long rentalDays) {
        if (rentalDays < 1) {
            throw new IllegalArgumentException("rentalDays must be at least 1");
        }
        return rentalDays >= 5 ? longTermPricingStrategy : normalPricingStrategy;
    }

    public record PricingLine(BigDecimal unitPrice, int quantity) {
        public PricingLine {
            Objects.requireNonNull(unitPrice, "unitPrice must not be null");
            if (unitPrice.signum() <= 0 || quantity <= 0) {
                throw new IllegalArgumentException("unitPrice and quantity must be valid");
            }
        }
    }

    public record PricingResult(long rentalDays, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    }
}
