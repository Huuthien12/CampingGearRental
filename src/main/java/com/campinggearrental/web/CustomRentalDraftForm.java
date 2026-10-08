package com.campinggearrental.web;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

public class CustomRentalDraftForm {
    private String customerId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate rentalDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate expectedReturnDate;
    private List<CustomRentalItemForm> items = new ArrayList<>();

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public LocalDate getRentalDate() { return rentalDate; }
    public void setRentalDate(LocalDate rentalDate) { this.rentalDate = rentalDate; }
    public LocalDate getExpectedReturnDate() { return expectedReturnDate; }
    public void setExpectedReturnDate(LocalDate expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }
    public List<CustomRentalItemForm> getItems() { return items; }
    public void setItems(List<CustomRentalItemForm> items) { this.items = items; }
}
