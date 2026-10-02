package com.campinggearrental.repository;

import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalDetail;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.model.RentalOrderStatus;
import com.campinggearrental.singleton.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class JdbcRentalOrderRepository implements RentalOrderRepository {
    private static final String ORDER_COLUMNS = "id, customer_id, rental_date, expected_return_date, actual_return_date, "
            + "status, subtotal, discount, total, payment_status, created_at";

    @Override
    public void insert(RentalOrder rentalOrder) throws SQLException {
        validateOrderFields(rentalOrder);
        Objects.requireNonNull(rentalOrder.getDetails(), "rental details must not be null");
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                insert(connection, rentalOrder);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    /**
     * Saves through a caller-owned transaction; the caller is responsible for commit or rollback.
     */
    public void insert(Connection connection, RentalOrder rentalOrder) throws SQLException {
        Objects.requireNonNull(connection, "connection must not be null");
        validateOrderFields(rentalOrder);
        Objects.requireNonNull(rentalOrder.getDetails(), "rental details must not be null");
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO rental_orders (" + ORDER_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, COALESCE(?, CURRENT_TIMESTAMP))")) {
            bindOrder(statement, rentalOrder);
            statement.executeUpdate();
        }
        for (RentalDetail detail : rentalOrder.getDetails()) {
            insertDetail(connection, rentalOrder.getId(), detail);
        }
    }

    @Override
    public Optional<RentalOrder> findById(String id) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT " + ORDER_COLUMNS + " FROM rental_orders WHERE id = ?")) {
            statement.setString(1, id);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(readOrder(connection, results)) : Optional.empty();
            }
        }
    }

    @Override
    public List<RentalOrder> findAll() throws SQLException {
        List<RentalOrder> orders = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT " + ORDER_COLUMNS + " FROM rental_orders ORDER BY created_at DESC");
             ResultSet results = statement.executeQuery()) {
            while (results.next()) orders.add(readOrder(connection, results));
        }
        return orders;
    }

    @Override
    public void update(RentalOrder rentalOrder) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            update(connection, rentalOrder);
        }
    }

    /** Updates order-level lifecycle, pricing, payment, and date fields in a caller-owned transaction. */
    public void update(Connection connection, RentalOrder rentalOrder) throws SQLException {
        Objects.requireNonNull(connection, "connection must not be null");
        validateOrderFields(rentalOrder);
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE rental_orders SET customer_id=?, rental_date=?, expected_return_date=?, actual_return_date=?, "
                        + "status=?, subtotal=?, discount=?, total=?, payment_status=? WHERE id=?")) {
            statement.setString(1, rentalOrder.getCustomerId());
            statement.setDate(2, Date.valueOf(rentalOrder.getRentalDate()));
            statement.setDate(3, Date.valueOf(rentalOrder.getExpectedReturnDate()));
            setNullableDate(statement, 4, rentalOrder.getActualReturnDate());
            statement.setString(5, RentalOrderStatus.fromState(rentalOrder.getCurrentState()).name());
            statement.setBigDecimal(6, rentalOrder.getSubtotal());
            statement.setBigDecimal(7, rentalOrder.getDiscount());
            statement.setBigDecimal(8, rentalOrder.getTotal());
            statement.setString(9, paymentStatus(rentalOrder.getPaymentStatus()).name());
            statement.setString(10, rentalOrder.getId());
            statement.executeUpdate();
        }
    }

    static RentalOrder mapOrder(String id, String customerId, LocalDate rentalDate, LocalDate expectedReturnDate,
            LocalDate actualReturnDate, String status, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
            String paymentStatus, LocalDateTime createdAt) {
        RentalOrder order = new RentalOrder();
        order.setId(id);
        order.setCustomerId(customerId);
        order.setRentalDate(rentalDate);
        order.setExpectedReturnDate(expectedReturnDate);
        order.setActualReturnDate(actualReturnDate);
        order.setCurrentState(RentalOrderStatus.fromPersisted(status).toState());
        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setTotal(total);
        order.setPaymentStatus(paymentStatus(paymentStatus));
        order.setCreatedAt(createdAt);
        return order;
    }

    static RentalDetail mapDetail(Long id, String rentalOrderId, String equipmentId, int quantity, BigDecimal unitPrice) {
        RentalDetail detail = new RentalDetail();
        detail.setId(id);
        detail.setRentalOrderId(rentalOrderId);
        detail.setEquipmentId(equipmentId);
        detail.setQuantity(quantity);
        detail.setUnitPrice(unitPrice);
        return detail;
    }

    private RentalOrder readOrder(Connection connection, ResultSet results) throws SQLException {
        Timestamp createdAt = results.getTimestamp("created_at");
        RentalOrder order = mapOrder(results.getString("id"), results.getString("customer_id"),
                results.getDate("rental_date").toLocalDate(), results.getDate("expected_return_date").toLocalDate(),
                date(results, "actual_return_date"), results.getString("status"), results.getBigDecimal("subtotal"),
                results.getBigDecimal("discount"), results.getBigDecimal("total"), results.getString("payment_status"),
                createdAt == null ? null : createdAt.toLocalDateTime());
        order.setDetails(readDetails(connection, order.getId()));
        return order;
    }

    private List<RentalDetail> readDetails(Connection connection, String orderId) throws SQLException {
        List<RentalDetail> details = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, rental_order_id, equipment_id, quantity, unit_price FROM rental_details WHERE rental_order_id = ? ORDER BY id")) {
            statement.setString(1, orderId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) details.add(mapDetail(results.getLong("id"), results.getString("rental_order_id"),
                        results.getString("equipment_id"), results.getInt("quantity"), results.getBigDecimal("unit_price")));
            }
        }
        return details;
    }

    private void bindOrder(PreparedStatement statement, RentalOrder order) throws SQLException {
        statement.setString(1, order.getId());
        statement.setString(2, order.getCustomerId());
        statement.setDate(3, Date.valueOf(order.getRentalDate()));
        statement.setDate(4, Date.valueOf(order.getExpectedReturnDate()));
        setNullableDate(statement, 5, order.getActualReturnDate());
        statement.setString(6, RentalOrderStatus.fromState(order.getCurrentState()).name());
        statement.setBigDecimal(7, order.getSubtotal());
        statement.setBigDecimal(8, order.getDiscount());
        statement.setBigDecimal(9, order.getTotal());
        statement.setString(10, paymentStatus(order.getPaymentStatus()).name());
        if (order.getCreatedAt() == null) statement.setTimestamp(11, null);
        else statement.setTimestamp(11, Timestamp.valueOf(order.getCreatedAt()));
    }

    private void insertDetail(Connection connection, String orderId, RentalDetail detail) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO rental_details (rental_order_id, equipment_id, quantity, unit_price) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, orderId);
            statement.setString(2, detail.getEquipmentId());
            statement.setInt(3, detail.getQuantity());
            statement.setBigDecimal(4, detail.getUnitPrice());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) detail.setId(keys.getLong(1));
            }
            detail.setRentalOrderId(orderId);
        }
    }

    private static LocalDate date(ResultSet results, String column) throws SQLException {
        Date value = results.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private static void setNullableDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) statement.setNull(index, java.sql.Types.DATE);
        else statement.setDate(index, Date.valueOf(value));
    }

    private static PaymentStatus paymentStatus(String value) {
        if (value == null) throw new IllegalArgumentException("payment status must not be null");
        try {
            return PaymentStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown payment status: " + value, exception);
        }
    }

    private static PaymentStatus paymentStatus(PaymentStatus value) {
        return Objects.requireNonNull(value, "payment status must not be null");
    }

    private static void validateOrderFields(RentalOrder order) {
        Objects.requireNonNull(order, "rentalOrder must not be null");
        Objects.requireNonNull(order.getId(), "rental order id must not be null");
        Objects.requireNonNull(order.getCustomerId(), "customer id must not be null");
        Objects.requireNonNull(order.getRentalDate(), "rental date must not be null");
        Objects.requireNonNull(order.getExpectedReturnDate(), "expected return date must not be null");
        Objects.requireNonNull(order.getCurrentState(), "rental state must not be null");
        Objects.requireNonNull(order.getSubtotal(), "subtotal must not be null");
        Objects.requireNonNull(order.getDiscount(), "discount must not be null");
        Objects.requireNonNull(order.getTotal(), "total must not be null");
        Objects.requireNonNull(order.getPaymentStatus(), "payment status must not be null");
    }
}
