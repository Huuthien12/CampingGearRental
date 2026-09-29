package com.campinggearrental.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class RentalOrder {
    private LocalDate rentalDate;
    private LocalDate expectedReturnDate;
    private List<RentalDetail> details;

    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private PaymentStatus paymentStatus;

    public LocalDate getRentalDate() { return rentalDate; }
    public void setRentalDate(LocalDate rentalDate) { this.rentalDate = rentalDate; }

    public LocalDate getExpectedReturnDate() { return expectedReturnDate; }
    public void setExpectedReturnDate(LocalDate expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }

    public List<RentalDetail> getDetails() { return details; }
    public void setDetails(List<RentalDetail> details) { this.details = details; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    
    // (Sau này bạn thêm thuộc tính id, customerId, currentState cho State Pattern...)
}