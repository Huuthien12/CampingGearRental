package com.campinggearrental.web;

import com.campinggearrental.factory.CampingPackageType;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class RentalDraftForm {
    private String customerId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate rentalDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate expectedReturnDate;
    private CampingPackageType packageType;
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public LocalDate getRentalDate() { return rentalDate; }
    public void setRentalDate(LocalDate rentalDate) { this.rentalDate = rentalDate; }
    public LocalDate getExpectedReturnDate() { return expectedReturnDate; }
    public void setExpectedReturnDate(LocalDate expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }
    public CampingPackageType getPackageType() { return packageType; }
    public void setPackageType(CampingPackageType packageType) { this.packageType = packageType; }
}
