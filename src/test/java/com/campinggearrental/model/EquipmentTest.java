package com.campinggearrental.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EquipmentTest {
    private Equipment equipment() { return new Equipment("EQ001", "Tent", "CAT001", new BigDecimal("100.00"), 5); }

    @Test void newEquipmentStartsFullyAvailable() { Equipment e = equipment(); assertEquals(5, e.getAvailableQuantity()); }
    @Test void rejectsInvalidConstructionValues() {
        assertThrows(IllegalArgumentException.class, () -> new Equipment("E", "Tent", "C", BigDecimal.ZERO, 1));
        assertThrows(IllegalArgumentException.class, () -> new Equipment("E", "Tent", "C", BigDecimal.ONE, -1));
        assertThrows(IllegalArgumentException.class, () -> new Equipment("E", "Tent", "C", BigDecimal.ONE, 2, -1, EquipmentStatus.AVAILABLE));
        assertThrows(IllegalArgumentException.class, () -> new Equipment("E", "Tent", "C", BigDecimal.ONE, 2, 3, EquipmentStatus.AVAILABLE));
    }
    @Test void reservesOnlyAvailablePositiveStock() {
        Equipment e = equipment(); assertTrue(e.hasEnoughStock(5)); assertFalse(e.hasEnoughStock(6)); assertFalse(e.hasEnoughStock(0));
        e.reserve(2); assertEquals(3, e.getAvailableQuantity());
        assertThrows(IllegalArgumentException.class, () -> e.reserve(0)); assertThrows(IllegalStateException.class, () -> e.reserve(4));
    }
    @Test void releasesWithoutExceedingTotal() {
        Equipment e = equipment(); e.reserve(2); e.release(2); assertEquals(5, e.getAvailableQuantity());
        assertThrows(IllegalArgumentException.class, () -> e.release(0)); assertThrows(IllegalStateException.class, () -> e.release(1));
    }
    @Test void adjustsTotalWhilePreservingReservedQuantity() {
        Equipment e = equipment(); e.reserve(2); e.adjustTotalQuantity(8); assertEquals(8, e.getTotalQuantity()); assertEquals(6, e.getAvailableQuantity());
        e.adjustTotalQuantity(3); assertEquals(3, e.getTotalQuantity()); assertEquals(1, e.getAvailableQuantity());
        assertThrows(IllegalStateException.class, () -> e.adjustTotalQuantity(1));
        assertTrue(e.getAvailableQuantity() >= 0 && e.getAvailableQuantity() <= e.getTotalQuantity());
    }
}
