package com.campinggearrental.factory;

import java.util.List;

public final class SoloCampingPackageCreator extends CampingPackageCreator {
    @Override
    public CampingPackage createPackage() {
        return new CampingPackage(CampingPackageType.SOLO, List.of(
                new CampingPackageItem("EQ001", 1),
                new CampingPackageItem("EQ002", 1),
                new CampingPackageItem("EQ004", 1)));
    }
}
