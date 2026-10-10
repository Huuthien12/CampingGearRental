package com.campinggearrental.factory;

import java.util.List;

public final class FamilyCampingPackageCreator extends CampingPackageCreator {
    @Override
    public CampingPackage createPackage() {
        return new CampingPackage(CampingPackageType.FAMILY, List.of(
                new CampingPackageItem("EQ001", 1),
                new CampingPackageItem("EQ002", 4),
                new CampingPackageItem("EQ003", 4),
                new CampingPackageItem("EQ004", 2),
                new CampingPackageItem("EQ005", 1)));
    }
}
