package com.campinggearrental.service;

import static org.junit.jupiter.api.Assertions.*;

import com.campinggearrental.model.Equipment;
import com.campinggearrental.repository.EquipmentRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
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
    @Test void retriesOnlyConfirmedDuplicateEquipmentPrimaryKeys() throws Exception {
        InsertRepository repository = new InsertRepository();
        repository.failures.add(new SQLException("Duplicate entry 'EQ-00000001' for key 'equipment.PRIMARY'", "23000", 1062));
        Deque<String> ids = new ArrayDeque<>(List.of("EQ-00000001", "EQ-00000002"));
        EquipmentService service = new EquipmentService(repository, () -> { throw new AssertionError("not used"); }, ids::removeFirst);

        Equipment created = service.create(new EquipmentService.CatalogUpdate("Tent", "CAT001", BigDecimal.ONE, 1, com.campinggearrental.model.EquipmentStatus.AVAILABLE));

        assertEquals("EQ-00000002", created.getEquipmentId());
        assertEquals(2, repository.insertAttempts);
    }
    @Test void failsAfterBoundedDuplicateEquipmentPrimaryKeyRetries() {
        InsertRepository repository = new InsertRepository();
        for (int index = 0; index < 5; index++) repository.failures.add(new SQLException("Duplicate entry for key 'PRIMARY'", "23000", 1062));
        EquipmentService service = new EquipmentService(repository, () -> { throw new AssertionError("not used"); }, () -> "EQ-00000001");

        assertThrows(SQLException.class, () -> service.create(new EquipmentService.CatalogUpdate("Tent", "CAT001", BigDecimal.ONE, 1, com.campinggearrental.model.EquipmentStatus.AVAILABLE)));
        assertEquals(5, repository.insertAttempts);
    }
    @Test void propagatesUnrelatedIntegrityErrorsWithoutRetrying() {
        InsertRepository repository = new InsertRepository();
        SQLException failure = new SQLException("Cannot add or update a child row", "23000", 1452);
        repository.failures.add(failure);
        EquipmentService service = new EquipmentService(repository, () -> { throw new AssertionError("not used"); }, () -> "EQ-00000001");

        SQLException thrown = assertThrows(SQLException.class, () -> service.create(new EquipmentService.CatalogUpdate("Tent", "CAT001", BigDecimal.ONE, 1, com.campinggearrental.model.EquipmentStatus.AVAILABLE)));
        assertSame(failure, thrown);
        assertEquals(1, repository.insertAttempts);
    }
    private static class MemoryRepository implements EquipmentRepository {
        private final List<Equipment> values = new ArrayList<>();
        public List<Equipment> findAll() { return List.copyOf(values); }
        public Optional<Equipment> findById(String id) { return values.stream().filter(e -> e.getEquipmentId().equals(id)).findFirst(); }
        public List<Equipment> search(String keyword) { return values.stream().filter(e -> e.getName().toLowerCase().contains(keyword.toLowerCase())).toList(); }
        public void insert(Equipment equipment) throws SQLException { values.add(equipment); }
        public void update(Equipment equipment) { }
    }
    private static class InsertRepository extends MemoryRepository {
        final Deque<SQLException> failures = new ArrayDeque<>();
        int insertAttempts;
        @Override public void insert(Equipment equipment) throws SQLException {
            insertAttempts++;
            if (!failures.isEmpty()) throw failures.removeFirst();
            super.insert(equipment);
        }
    }
}
