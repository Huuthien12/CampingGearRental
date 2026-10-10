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
import com.campinggearrental.singleton.DatabaseConnection;
import com.campinggearrental.state.ConfirmedState;
import com.campinggearrental.state.PendingState;
import com.campinggearrental.state.RentalState;
import com.campinggearrental.state.RentedState;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Connection;
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
    private final ConnectionProvider connectionProvider;

    public RentalService(CustomerRepository customerRepository, EquipmentRepository equipmentRepository,
            RentalOrderRepository rentalOrderRepository) {
        this.customerRepository = Objects.requireNonNull(customerRepository);
        this.equipmentRepository = Objects.requireNonNull(equipmentRepository);
        this.rentalOrderRepository = Objects.requireNonNull(rentalOrderRepository);
        this.connectionProvider = DatabaseConnection.getInstance()::getConnection;
    }

    RentalService(CustomerRepository customerRepository, EquipmentRepository equipmentRepository,
            RentalOrderRepository rentalOrderRepository, ConnectionProvider connectionProvider) {
        this.customerRepository = Objects.requireNonNull(customerRepository);
        this.equipmentRepository = Objects.requireNonNull(equipmentRepository);
        this.rentalOrderRepository = Objects.requireNonNull(rentalOrderRepository);
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
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

    public RentalOrder confirmRental(String id) throws SQLException {
        return transaction((connection, undo) -> {
            RentalOrder order = requireOrder(connection, id);
            requireState(order, PendingState.class, "only pending orders can be confirmed");
            List<Equipment> equipment = loadAvailableEquipment(connection, order);
            for (int index = 0; index < equipment.size(); index++) {
                Equipment item = equipment.get(index);
                int quantity = order.getDetails().get(index).getQuantity();
                item.reserve(quantity);
                undo.add(() -> item.release(quantity));
            }
            persistEquipment(connection, equipment);
            transition(order, undo, RentalOrder::confirmOrder);
            rentalOrderRepository.update(connection, order);
            return order;
        });
    }

    public RentalOrder rentRental(String id) throws SQLException {
        return transaction((connection, undo) -> {
            RentalOrder order = requireOrder(connection, id);
            requireState(order, ConfirmedState.class, "only confirmed orders can be rented");
            transition(order, undo, RentalOrder::rentEquipment);
            rentalOrderRepository.update(connection, order);
            return order;
        });
    }

    public RentalOrder cancelRental(String id) throws SQLException {
        return transaction((connection, undo) -> {
            RentalOrder order = requireOrder(connection, id);
            if (order.getCurrentState() instanceof PendingState) {
                transition(order, undo, RentalOrder::cancelOrder);
            } else if (order.getCurrentState() instanceof ConfirmedState) {
                List<Equipment> equipment = loadEquipment(connection, order);
                for (int index = 0; index < equipment.size(); index++) {
                    Equipment item = equipment.get(index);
                    int quantity = order.getDetails().get(index).getQuantity();
                    item.release(quantity);
                    undo.add(() -> item.reserve(quantity));
                }
                persistEquipment(connection, equipment);
                transition(order, undo, RentalOrder::cancelOrder);
            } else {
                throw new IllegalStateException("order cannot be cancelled in its current state");
            }
            rentalOrderRepository.update(connection, order);
            return order;
        });
    }

    public RentalOrder returnRental(String id, LocalDate actualReturnDate) throws SQLException {
        if (actualReturnDate == null) throw new IllegalArgumentException("actual return date must not be null");
        return transaction((connection, undo) -> {
            RentalOrder order = requireOrder(connection, id);
            requireState(order, RentedState.class, "only rented orders can be returned");
            if (actualReturnDate.isBefore(order.getRentalDate())) throw new IllegalArgumentException("actual return date must not be before rental date");
            List<Equipment> equipment = loadEquipment(connection, order);
            for (int index = 0; index < equipment.size(); index++) {
                Equipment item = equipment.get(index);
                int quantity = order.getDetails().get(index).getQuantity();
                item.release(quantity);
                undo.add(() -> item.reserve(quantity));
            }
            persistEquipment(connection, equipment);
            LocalDate oldDate = order.getActualReturnDate();
            undo.add(() -> order.setActualReturnDate(oldDate));
            order.setActualReturnDate(actualReturnDate);
            transition(order, undo, RentalOrder::returnEquipment);
            rentalOrderRepository.update(connection, order);
            return order;
        });
    }

    private RentalOrder requireOrder(Connection connection, String id) throws SQLException {
        return rentalOrderRepository.findByIdForUpdate(connection, requiredId(id, "rental order id"))
                .orElseThrow(() -> new IllegalArgumentException("rental order not found: " + id));
    }

    private List<Equipment> loadAvailableEquipment(Connection connection, RentalOrder order) throws SQLException {
        List<Equipment> equipment = loadEquipment(connection, order);
        for (int index = 0; index < equipment.size(); index++) {
            Equipment item = equipment.get(index);
            int quantity = order.getDetails().get(index).getQuantity();
            if (item.getStatus() != EquipmentStatus.AVAILABLE) throw new IllegalStateException("equipment is inactive: " + item.getEquipmentId());
            if (!item.hasEnoughStock(quantity)) throw new IllegalStateException("insufficient available equipment: " + item.getEquipmentId());
        }
        return equipment;
    }

    private List<Equipment> loadEquipment(Connection connection, RentalOrder order) throws SQLException {
        for (RentalDetail detail : order.getDetails()) {
            if (detail.getQuantity() <= 0) throw new IllegalArgumentException("rental detail quantity must be positive");
        }
        // Acquire distinct row locks in one order across transactions, without reordering detail/quantity pairs.
        LinkedHashMap<String, Equipment> locked = new LinkedHashMap<>();
        List<String> ids = order.getDetails().stream().map(RentalDetail::getEquipmentId).distinct().sorted().toList();
        for (String id : ids) {
            locked.put(id, equipmentRepository.findByIdForUpdate(connection, id)
                    .orElseThrow(() -> new IllegalArgumentException("equipment not found: " + id)));
        }
        List<Equipment> equipment = new ArrayList<>();
        for (RentalDetail detail : order.getDetails()) equipment.add(locked.get(detail.getEquipmentId()));
        return equipment;
    }

    private void persistEquipment(Connection connection, List<Equipment> equipment) throws SQLException {
        for (Equipment item : equipment) equipmentRepository.update(connection, item);
    }

    private static void requireState(RentalOrder order, Class<? extends RentalState> expected, String message) {
        if (!expected.isInstance(order.getCurrentState())) throw new IllegalStateException(message);
    }

    private static void transition(RentalOrder order, List<Runnable> undo, Transition transition) {
        RentalState oldState = order.getCurrentState();
        undo.add(() -> order.setCurrentState(oldState));
        transition.apply(order);
    }

    private <T> T transaction(TransactionWork<T> work) throws SQLException {
        List<Runnable> undo = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.apply(connection, undo);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } finally {
                    for (int index = undo.size() - 1; index >= 0; index--) undo.get(index).run();
                }
                throw exception;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
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
    @FunctionalInterface interface ConnectionProvider { Connection getConnection() throws SQLException; }
    @FunctionalInterface private interface Transition { void apply(RentalOrder order); }
    @FunctionalInterface private interface TransactionWork<T> { T apply(Connection connection, List<Runnable> undo) throws SQLException; }
}
