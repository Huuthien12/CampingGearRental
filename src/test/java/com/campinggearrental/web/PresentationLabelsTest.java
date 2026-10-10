package com.campinggearrental.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PresentationLabelsTest {
    private final PresentationLabels labels = new PresentationLabels();

    @Test void translatesKnownPersistedDisplayValues() {
        assertEquals("Chờ xác nhận", labels.rentalState(RentalOrderStatus.PENDING));
        assertEquals("Đang cho thuê", labels.rentalState("RENTED"));
        assertEquals("Đã thanh toán", labels.paymentStatus(PaymentStatus.PAID));
        assertEquals("Sẵn sàng cho thuê", labels.equipmentStatus(EquipmentStatus.AVAILABLE));
        assertEquals("Túi ngủ", labels.category("Sleeping Bag"));
    }

    @Test void preservesUnknownValuesAsSafeFallbacks() {
        assertEquals("ARCHIVED", labels.rentalState("ARCHIVED"));
        assertEquals("PARTIAL", labels.paymentStatus("PARTIAL"));
        assertEquals("Repair", labels.category("Repair"));
        assertEquals("", labels.equipmentStatus((String) null));
    }

    @Test void formatsMoneyForVietnamesePresentationWithoutChangingValues() {
        assertEquals("520.000 ₫", labels.money(new BigDecimal("520000.00")));
        assertEquals("1.234,5 ₫", labels.money(new BigDecimal("1234.50")));
        assertEquals("", labels.money(null));
    }

    @Test void shortensOnlyLongRentalIdsWhileKeepingTheirIdentityAvailableToTemplates() {
        assertEquals("RENT001", labels.rentalId("RENT001"));
        assertEquals("RENT2026…", labels.rentalId("RENT202612345678"));
    }

    @Test void formatsDatesAndFallsBackWithoutChangingStoredIdentifiers() {
        assertEquals("09/10/2026", labels.date(LocalDate.of(2026, 10, 9)));
        assertEquals("", labels.date(null));
        assertEquals("Nguyễn An", labels.fallback("Nguyễn An", "CUS001"));
        assertEquals("CUS001", labels.fallback("", "CUS001"));
    }

    @Test void describesInventoryReservationOnlyFromTheRentalState() {
        assertEquals("Chưa giữ thiết bị.", labels.inventoryNote("PENDING"));
        assertEquals("Đã giữ thiết bị.", labels.inventoryNote("CONFIRMED"));
        assertEquals("Thiết bị đang được cho thuê.", labels.inventoryNote("RENTED"));
        assertEquals("Thiết bị đã được trả, tồn kho đã hoàn lại.", labels.inventoryNote("RETURNED"));
        assertEquals("Đơn đã hủy, không còn giữ thiết bị.", labels.inventoryNote("CANCELLED"));
        assertEquals("Không xác định trạng thái giữ thiết bị.", labels.inventoryNote("UNKNOWN"));
    }
}
