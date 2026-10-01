package com.campinggearrental.service;

import com.campinggearrental.model.RentalOrder;
import java.util.Objects;

public class RentalPricingAdapter {
    private final PricingService pricingService;

    public RentalPricingAdapter(PricingService pricingService) {
        this.pricingService = Objects.requireNonNull(pricingService, "pricingService must not be null");
    }

    public PricingService.PricingResult calculateAndApply(RentalOrder rentalOrder) {
        Objects.requireNonNull(rentalOrder, "rentalOrder must not be null");
        PricingService.PricingResult result = pricingService.calculate(
                rentalOrder.getDetails().stream()
                        .map(detail -> new PricingService.PricingLine(detail.getUnitPrice(), detail.getQuantity()))
                        .toList(),
                rentalOrder.getRentalDate(),
                rentalOrder.getExpectedReturnDate());
        rentalOrder.setSubtotal(result.subtotal());
        rentalOrder.setDiscount(result.discount());
        rentalOrder.setTotal(result.total());
        return result;
    }
}
