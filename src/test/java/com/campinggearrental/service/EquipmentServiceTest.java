package com.campinggearrental.service;

import static org.junit.jupiter.api.Assertions.*;

import com.campinggearrental.model.Equipment;
import com.campinggearrental.repository.EquipmentRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EquipmentServiceTest {
    @Test void createsSearchesAndChangesStock() throws Exception {
        MemoryRepository repository = new MemoryRepository();
        EquipmentService service = new EquipmentService(repository);
        Equipment equipment = new Equipment("EQ001", "Tent", "CAT001", new BigDecimal("100"), 5);
        service.create(equipment);
        assertEquals(1, service.search("tent").size());
        service.reserve("EQ001", 2); assertEquals(3, equipment.getAvailableQuantity());
        service.release("EQ001", 1); assertEquals(4, equipment.getAvailableQuantity());
        service.adjustTotalQuantity("EQ001", 6); assertEquals(6, equipment.getTotalQuantity());
    }
    @Test void rejectsInsufficientOrUnknownEquipment() throws Exception {
        MemoryRepository repository = new MemoryRepository();
        EquipmentService service = new EquipmentService(repository);
        service.create(new Equipment("EQ001", "Tent", "CAT001", BigDecimal.ONE, 1));
        assertThrows(IllegalStateException.class, () -> service.reserve("EQ001", 2));
        assertThrows(IllegalArgumentException.class, () -> service.reserve("missing", 1));
    }
    private static class MemoryRepository implements EquipmentRepository {
        private final List<Equipment> values = new ArrayList<>();
        public List<Equipment> findAll() { return List.copyOf(values); }
        public Optional<Equipment> findById(String id) { return values.stream().filter(e -> e.getEquipmentId().equals(id)).findFirst(); }
        public List<Equipment> search(String keyword) { return values.stream().filter(e -> e.getName().toLowerCase().contains(keyword.toLowerCase())).toList(); }
        public void insert(Equipment equipment) { values.add(equipment); }
        public void update(Equipment equipment) { }
    }
}
