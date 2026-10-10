package com.campinggearrental.factory;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.List;

class CampingPackageFactoryTest {
    @Test void selectorReturnsTheConcreteCreatorForEachPackageType() {
        assertInstanceOf(SoloCampingPackageCreator.class, CampingPackageFactory.creatorFor(CampingPackageType.SOLO));
        assertInstanceOf(CoupleCampingPackageCreator.class, CampingPackageFactory.creatorFor(CampingPackageType.COUPLE));
        assertInstanceOf(FamilyCampingPackageCreator.class, CampingPackageFactory.creatorFor(CampingPackageType.FAMILY));
    }
    @Test void eachConcreteCreatorBuildsItsOwnPackageVariant() {
        assertEquals(CampingPackageType.SOLO, new SoloCampingPackageCreator().createPackage().type());
        assertEquals(CampingPackageType.COUPLE, new CoupleCampingPackageCreator().createPackage().type());
        assertEquals(CampingPackageType.FAMILY, new FamilyCampingPackageCreator().createPackage().type());
    }
    @Test void factoryMethodDispatchesPolymorphically() {
        CampingPackageCreator creator = CampingPackageFactory.creatorFor(CampingPackageType.COUPLE);
        CampingPackage result = creator.createPackage();
        assertEquals(CampingPackageType.COUPLE, result.type());
        assertEquals(new CampingPackageItem("EQ002", 2), result.items().get(1));
    }
    @Test void rejectsMissingPackageType() { assertThrows(IllegalArgumentException.class, () -> CampingPackageFactory.creatorFor(null)); }

    @ParameterizedTest
    @EnumSource(CampingPackageType.class)
    void preservesFullCompositionAndOrder(CampingPackageType type) {
        CampingPackageCreator creator = CampingPackageFactory.creatorFor(type);
        CampingPackage result = creator.createPackage();
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
        var items = CampingPackageFactory.creatorFor(type).createPackage().items();
        assertThrows(UnsupportedOperationException.class, () -> items.add(new CampingPackageItem("OTHER", 1)));
        assertThrows(UnsupportedOperationException.class, () -> items.set(0, new CampingPackageItem("OTHER", 1)));
        assertThrows(UnsupportedOperationException.class, items::clear);
    }
}
