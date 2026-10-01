package com.campinggearrental.factory;

public record CampingPackageItem(String equipmentId, int quantity) {
    public CampingPackageItem {
        if (equipmentId == null || equipmentId.isBlank() || quantity <= 0) throw new IllegalArgumentException("valid package item required");
    }
}
