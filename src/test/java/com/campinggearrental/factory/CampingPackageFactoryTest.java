package com.campinggearrental.factory;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.List;

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

    @ParameterizedTest
    @EnumSource(CampingPackageType.class)
    void preservesFullCompositionAndOrder(CampingPackageType type) {
        CampingPackage result = CampingPackageFactory.create(type);
        assertEquals(type, result.type());
        List<CampingPackageItem> expected = switch (type) {
            case SOLO -> List.of(new CampingPackageItem("EQ001", 1), new CampingPackageItem("EQ002", 1),
                    new CampingPackageItem("EQ004", 1));
            case COUPLE -> List.of(new CampingPackageItem("EQ001", 1), new CampingPackageItem("EQ002", 2),
                    new CampingPackageItem("EQ003", 2), new CampingPackageItem("EQ004", 1));
            case FAMILY -> List.of(new CampingPackageItem("EQ001", 1), new CampingPackageItem("EQ002", 4),
                    new CampingPackageItem("EQ003", 4), new CampingPackageItem("EQ004", 2),
                    new CampingPackageItem("EQ005", 1));
        };
        assertEquals(expected, result.items());
    }

    @ParameterizedTest
    @EnumSource(CampingPackageType.class)
    void compositionCannotBeMutatedByCaller(CampingPackageType type) {
        var items = CampingPackageFactory.create(type).items();
        assertThrows(UnsupportedOperationException.class, () -> items.add(new CampingPackageItem("OTHER", 1)));
        assertThrows(UnsupportedOperationException.class, () -> items.set(0, new CampingPackageItem("OTHER", 1)));
        assertThrows(UnsupportedOperationException.class, items::clear);
    }
}
