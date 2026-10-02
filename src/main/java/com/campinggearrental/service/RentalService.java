package com.campinggearrental.service;

import com.campinggearrental.model.Customer;
import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalDetail;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.repository.CustomerRepository;
import com.campinggearrental.repository.EquipmentRepository;
import com.campinggearrental.repository.RentalOrderRepository;
import com.campinggearrental.state.PendingState;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Creates persisted rental drafts; lifecycle and inventory mutation belong to later checkpoints. */
public class RentalService {
    private final CustomerRepository customerRepository;
    private final EquipmentRepository equipmentRepository;
    private final RentalOrderRepository rentalOrderRepository;

    public RentalService(CustomerRepository customerRepository, EquipmentRepository equipmentRepository,
            RentalOrderRepository rentalOrderRepository) {
        this.customerRepository = Objects.requireNonNull(customerRepository);
        this.equipmentRepository = Objects.requireNonNull(equipmentRepository);
        this.rentalOrderRepository = Objects.requireNonNull(rentalOrderRepository);
    }

    public RentalOrder createDraft(RentalRequest request) throws SQLException {
        Objects.requireNonNull(request, "rental request must not be null");
        String customerId = requiredId(request.customerId(), "customer id");
        validateDates(request.rentalDate(), request.expectedReturnDate());
        requireCustomer(customerId);

        List<RentalDetail> details = new ArrayList<>();
        for (var item : aggregate(request.items()).entrySet()) {
            Equipment equipment = equipmentRepository.findById(item.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("equipment not found: " + item.getKey()));
            if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
                throw new IllegalArgumentException("equipment is inactive: " + item.getKey());
            }
            if (!equipment.hasEnoughStock(item.getValue())) {
                throw new IllegalArgumentException("insufficient available equipment: " + item.getKey());
            }
            details.add(detail(equipment, item.getValue()));
        }

        RentalOrder order = new RentalOrder();
        order.setId("RENT" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        order.setCustomerId(customerId);
        order.setRentalDate(request.rentalDate());
        order.setExpectedReturnDate(request.expectedReturnDate());
        order.setActualReturnDate(null);
        order.setCurrentState(new PendingState());
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setSubtotal(BigDecimal.ZERO);
        order.setDiscount(BigDecimal.ZERO);
        order.setTotal(BigDecimal.ZERO);
        order.setDetails(details);
        rentalOrderRepository.insert(order);
        return order;
    }

    private void requireCustomer(String customerId) throws SQLException {
        boolean exists = customerRepository.findAll().stream().map(Customer::id).anyMatch(customerId::equals);
        if (!exists) throw new IllegalArgumentException("customer not found: " + customerId);
    }

    private LinkedHashMap<String, Integer> aggregate(List<RentalRequestItem> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("at least one rental item is required");
        LinkedHashMap<String, Integer> quantities = new LinkedHashMap<>();
        for (RentalRequestItem item : items) {
            if (item == null) throw new IllegalArgumentException("rental item must not be null");
            String equipmentId = requiredId(item.equipmentId(), "equipment id");
            if (item.quantity() <= 0) throw new IllegalArgumentException("equipment quantity must be positive");
            quantities.merge(equipmentId, item.quantity(), Math::addExact);
        }
        return quantities;
    }

    private RentalDetail detail(Equipment equipment, int quantity) {
        RentalDetail detail = new RentalDetail();
        detail.setEquipmentId(equipment.getEquipmentId());
        detail.setQuantity(quantity);
        detail.setUnitPrice(equipment.getPricePerDay());
        return detail;
    }

    private static String requiredId(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value.trim();
    }

    private static void validateDates(LocalDate rentalDate, LocalDate expectedReturnDate) {
        if (rentalDate == null || expectedReturnDate == null) throw new IllegalArgumentException("rental dates must not be null");
        if (expectedReturnDate.isBefore(rentalDate)) throw new IllegalArgumentException("expected return date must not be before rental date");
    }

    public record RentalRequest(String customerId, LocalDate rentalDate, LocalDate expectedReturnDate,
                                List<RentalRequestItem> items) { }
    public record RentalRequestItem(String equipmentId, int quantity) { }
}
