package com.campinggearrental.service;

import static org.junit.jupiter.api.Assertions.*;

import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.repository.RentalOrderRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RentalOrderServiceTest {
    @Test void delegatesReadOperationsToPersistedRepository() throws Exception {
        RentalOrder order = new RentalOrder(); order.setId("RENT001");
        RentalOrderRepository repository = new RentalOrderRepository() {
            public void insert(RentalOrder value) { fail("unexpected write"); }
            public Optional<RentalOrder> findById(String id) { return Optional.of(order); }
            public List<RentalOrder> findAll() { return List.of(order); }
            public void update(RentalOrder value) { fail("unexpected write"); }
        };
        RentalOrderService service = new RentalOrderService(repository);
        assertEquals(List.of(order), service.findAll());
        assertSame(order, service.findById(" RENT001 "));
        assertNull(service.findById(" "));
    }
}
