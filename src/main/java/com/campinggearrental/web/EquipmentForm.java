package com.campinggearrental.web;

import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.service.EquipmentService.CatalogUpdate;
import java.math.BigDecimal;

/** Web input adapter. Domain constructors/methods remain the validation authority. */
public class EquipmentForm {
    private String equipmentId;
    private String name;
    private String categoryId;
    private BigDecimal pricePerDay;
    private Integer totalQuantity;
    private EquipmentStatus status = EquipmentStatus.AVAILABLE;

    public static EquipmentForm from(Equipment equipment) {
        EquipmentForm form = new EquipmentForm();
        form.equipmentId = equipment.getEquipmentId();
        form.name = equipment.getName();
        form.categoryId = equipment.getCategoryId();
        form.pricePerDay = equipment.getPricePerDay();
        form.totalQuantity = equipment.getTotalQuantity();
        form.status = equipment.getStatus();
        return form;
    }

    public Equipment toNewEquipment() {
        requireFields();
        Equipment equipment = new Equipment(equipmentId, name, categoryId, pricePerDay, totalQuantity);
        equipment.setStatus(status);
        return equipment;
    }

    public CatalogUpdate toCatalogUpdate() {
        requireFields();
        return new CatalogUpdate(name, categoryId, pricePerDay, totalQuantity, status);
    }
    public CatalogUpdate toNewCatalogUpdate() { return toCatalogUpdate(); }

    private void requireFields() {
        if (totalQuantity == null) throw new IllegalArgumentException("Total quantity is required.");
        if (status == null) throw new IllegalArgumentException("Status is required.");
    }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public BigDecimal getPricePerDay() { return pricePerDay; }
    public void setPricePerDay(BigDecimal pricePerDay) { this.pricePerDay = pricePerDay; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public EquipmentStatus getStatus() { return status; }
    public void setStatus(EquipmentStatus status) { this.status = status; }
}
