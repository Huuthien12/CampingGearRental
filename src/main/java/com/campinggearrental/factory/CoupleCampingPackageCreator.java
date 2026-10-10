package com.campinggearrental.factory;

import java.util.List;

public final class CoupleCampingPackageCreator extends CampingPackageCreator {
    @Override
    public CampingPackage createPackage() {
        return new CampingPackage(CampingPackageType.COUPLE, List.of(
                new CampingPackageItem("EQ001", 1),
                new CampingPackageItem("EQ002", 2),
                new CampingPackageItem("EQ003", 2),
                new CampingPackageItem("EQ004", 1)));
    }
}
