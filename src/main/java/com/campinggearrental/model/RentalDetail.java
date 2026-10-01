package com.campinggearrental.model;

import java.math.BigDecimal;

public class RentalDetail {
    private Long id;
    private String rentalOrderId;
    private String equipmentId;
    private int quantity;
    private BigDecimal unitPrice;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRentalOrderId() { return rentalOrderId; }
    public void setRentalOrderId(String rentalOrderId) { this.rentalOrderId = rentalOrderId; }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
