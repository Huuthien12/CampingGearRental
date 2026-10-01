package com.campinggearrental.factory;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CampingPackageFactoryTest {
    @Test void createsAllPlannedPackageTypes() {
        assertEquals(3, CampingPackageFactory.create(CampingPackageType.SOLO).items().size());
        assertEquals(4, CampingPackageFactory.create(CampingPackageType.COUPLE).items().size());
        assertEquals(5, CampingPackageFactory.create(CampingPackageType.FAMILY).items().size());
    }
    @Test void packageDefinitionsUseExpectedQuantities() {
        assertEquals(new CampingPackageItem("EQ002", 2), CampingPackageFactory.create(CampingPackageType.COUPLE).items().get(1));
        assertEquals(new CampingPackageItem("EQ005", 1), CampingPackageFactory.create(CampingPackageType.FAMILY).items().getLast());
    }
    @Test void rejectsMissingPackageType() { assertThrows(IllegalArgumentException.class, () -> CampingPackageFactory.create(null)); }
}
