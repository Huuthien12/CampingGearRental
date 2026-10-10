package com.campinggearrental.factory;

public final class CampingPackageFactory {
    private CampingPackageFactory() { }
    public static CampingPackageCreator creatorFor(CampingPackageType type) {
        if (type == null) throw new IllegalArgumentException("package type is required");
        return switch (type) {
            case SOLO -> new SoloCampingPackageCreator();
            case COUPLE -> new CoupleCampingPackageCreator();
            case FAMILY -> new FamilyCampingPackageCreator();
        };
    }
}
