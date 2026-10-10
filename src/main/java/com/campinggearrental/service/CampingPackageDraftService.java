package com.campinggearrental.service;

import com.campinggearrental.factory.CampingPackageFactory;
import com.campinggearrental.factory.CampingPackageCreator;
import com.campinggearrental.factory.CampingPackageType;
import com.campinggearrental.model.RentalOrder;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;

/** Maps server-defined packages into the existing rental draft workflow. */
public class CampingPackageDraftService {
    private final RentalService rentalService;

    public CampingPackageDraftService(RentalService rentalService) {
        this.rentalService = Objects.requireNonNull(rentalService);
    }

    public RentalOrder createDraftFromPackage(String customerId, LocalDate rentalDate,
            LocalDate expectedReturnDate, CampingPackageType packageType) throws SQLException {
        CampingPackageCreator creator = CampingPackageFactory.creatorFor(packageType);
        var campingPackage = creator.createPackage();
        var items = campingPackage.items().stream()
                .map(item -> new RentalService.RentalRequestItem(item.equipmentId(), item.quantity()))
                .toList();
        return rentalService.createDraft(new RentalService.RentalRequest(
                customerId, rentalDate, expectedReturnDate, items));
    }
}
