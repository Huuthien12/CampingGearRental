package com.campinggearrental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.campinggearrental.strategy.LongTermPricingStrategy;
import com.campinggearrental.strategy.NormalPricingStrategy;
import org.junit.jupiter.api.Test;

class PricingServiceTest {
    private final PricingService pricingService = new PricingService();
    private final LocalDate rentalDate = LocalDate.of(2026, 10, 20);

    @Test
    void calculatesOneDayForSameDateRental() {
        assertEquals(1, pricingService.calculateRentalDays(rentalDate, rentalDate));
    }

    @Test
    void calculatesOneDayForNextDateRental() {
        assertEquals(1, pricingService.calculateRentalDays(rentalDate, rentalDate.plusDays(1)));
    }

    @Test
    void calculatesDayDifferenceForLongerRentals() {
        assertEquals(3, pricingService.calculateRentalDays(rentalDate, rentalDate.plusDays(3)));
    }

    @Test
    void rejectsReturnDateBeforeRentalDate() {
        assertThrows(IllegalArgumentException.class,
                () -> pricingService.calculateRentalDays(rentalDate, rentalDate.minusDays(1)));
    }

    @Test
    void calculatesLineTotalWithBigDecimal() {
        assertEquals(new BigDecimal("600000.00"),
                pricingService.calculateLineTotal(new BigDecimal("100000.00"), 2, 3));
    }

    @Test
    void normalPricingHasNoDiscount() {
        assertEquals(BigDecimal.ZERO,
                new NormalPricingStrategy().calculateDiscount(new BigDecimal("540000.00"), 4));
    }

    @Test
    void longTermPricingHasNoDiscountBeforeFiveDays() {
        assertEquals(BigDecimal.ZERO,
                new LongTermPricingStrategy().calculateDiscount(new BigDecimal("540000.00"), 4));
    }

    @Test
    void longTermPricingDiscountsAtFiveDays() {
        BigDecimal subtotal = new BigDecimal("540000.00");
        assertEquals(new BigDecimal("54000.0000"), new LongTermPricingStrategy().calculateDiscount(subtotal, 5));
    }

    @Test
    void longTermPricingDiscountsBeyondFiveDays() {
        BigDecimal subtotal = new BigDecimal("540000.00");
        assertEquals(new BigDecimal("54000.0000"), new LongTermPricingStrategy().calculateDiscount(subtotal, 6));
    }

    @Test
    void calculatesSubtotalDiscountAndTotalFromNeutralLines() {
        PricingService.PricingResult result = pricingService.calculate(
                List.of(
                        new PricingService.PricingLine(new BigDecimal("100000.00"), 1),
                        new PricingService.PricingLine(new BigDecimal("30000.00"), 2)),
                rentalDate,
                rentalDate.plusDays(5));

        assertEquals(5, result.rentalDays());
        assertEquals(new BigDecimal("800000.00"), result.subtotal());
        assertEquals(new BigDecimal("80000.0000"), result.discount());
        assertEquals(new BigDecimal("720000.0000"), result.total());
    }

    @Test
    void keepsMonetaryResultsAsBigDecimal() {
        PricingService.PricingResult result = pricingService.calculate(
                List.of(new PricingService.PricingLine(new BigDecimal("100000.00"), 1)),
                rentalDate,
                rentalDate.plusDays(5));

        assertInstanceOf(BigDecimal.class, result.subtotal());
        assertInstanceOf(BigDecimal.class, result.discount());
        assertInstanceOf(BigDecimal.class, result.total());
    }
}
