package com.campinggearrental.factory;

import java.util.List;

public final class CampingPackageFactory {
    private CampingPackageFactory() { }
    public static CampingPackage create(CampingPackageType type) {
        if (type == null) throw new IllegalArgumentException("package type is required");
        return switch (type) {
            case SOLO -> new CampingPackage(type, List.of(item("EQ001", 1), item("EQ002", 1), item("EQ004", 1)));
            case COUPLE -> new CampingPackage(type, List.of(item("EQ001", 1), item("EQ002", 2), item("EQ003", 2), item("EQ004", 1)));
            case FAMILY -> new CampingPackage(type, List.of(item("EQ001", 1), item("EQ002", 4), item("EQ003", 4), item("EQ004", 2), item("EQ005", 1)));
        };
    }
    private static CampingPackageItem item(String id, int quantity) { return new CampingPackageItem(id, quantity); }
}
