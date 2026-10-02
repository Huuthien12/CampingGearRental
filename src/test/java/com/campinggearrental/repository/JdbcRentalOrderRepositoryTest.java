package com.campinggearrental.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.campinggearrental.model.RentalDetail;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.state.ConfirmedState;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class JdbcRentalOrderRepositoryTest {
    @Test
    void mapsEveryPersistedOrderField() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 2, 10, 30);
        RentalOrder order = JdbcRentalOrderRepository.mapOrder("RENT001", "CUS001", LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 6), "CONFIRMED", new BigDecimal("400.00"),
                new BigDecimal("40.00"), new BigDecimal("360.00"), "PAID", createdAt);

        assertEquals("RENT001", order.getId());
        assertEquals("CUS001", order.getCustomerId());
        assertEquals(LocalDate.of(2026, 10, 1), order.getRentalDate());
        assertEquals(LocalDate.of(2026, 10, 5), order.getExpectedReturnDate());
        assertEquals(LocalDate.of(2026, 10, 6), order.getActualReturnDate());
        assertInstanceOf(ConfirmedState.class, order.getCurrentState());
        assertEquals(new BigDecimal("400.00"), order.getSubtotal());
        assertEquals(new BigDecimal("40.00"), order.getDiscount());
        assertEquals(new BigDecimal("360.00"), order.getTotal());
        assertEquals(com.campinggearrental.model.PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(createdAt, order.getCreatedAt());
    }

    @Test
    void mapsDetailSnapshotsWithoutCatalogPriceAccess() {
        RentalDetail first = JdbcRentalOrderRepository.mapDetail(1L, "RENT001", "EQ001", 2, new BigDecimal("100000.00"));
        RentalDetail second = JdbcRentalOrderRepository.mapDetail(2L, "RENT001", "EQ002", 1, new BigDecimal("40000.00"));

        assertEquals("RENT001", first.getRentalOrderId());
        assertEquals("EQ001", first.getEquipmentId());
        assertEquals(2, first.getQuantity());
        assertEquals(new BigDecimal("100000.00"), first.getUnitPrice());
        assertEquals(2L, second.getId());
        assertEquals(new BigDecimal("40000.00"), second.getUnitPrice());
    }

    @Test
    void rejectsInvalidPersistedStatuses() {
        assertThrows(IllegalArgumentException.class, () -> JdbcRentalOrderRepository.mapOrder("RENT001", "CUS001",
                LocalDate.now(), LocalDate.now(), null, "UNKNOWN", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "UNPAID", null));
        assertThrows(IllegalArgumentException.class, () -> JdbcRentalOrderRepository.mapOrder("RENT001", "CUS001",
                LocalDate.now(), LocalDate.now(), null, "PENDING", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "UNKNOWN", null));
    }
}
