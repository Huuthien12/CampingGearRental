package com.campinggearrental.factory;

import java.util.List;

public record CampingPackage(CampingPackageType type, List<CampingPackageItem> items) {
    public CampingPackage { items = List.copyOf(items); }
}
