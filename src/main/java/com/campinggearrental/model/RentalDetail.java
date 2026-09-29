package com.campinggearrental.model;

import java.math.BigDecimal;

public class RentalDetail {
    private int quantity;
    private BigDecimal unitPrice;

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    
    // (Sau này bạn thêm các trường như id, rentalOrderId, equipmentId...)
}