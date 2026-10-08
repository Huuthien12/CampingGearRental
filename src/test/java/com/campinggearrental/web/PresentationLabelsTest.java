package com.campinggearrental.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalOrderStatus;
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
}
