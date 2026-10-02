package com.campinggearrental.service;

import static org.junit.jupiter.api.Assertions.*;

import com.campinggearrental.model.Customer;
import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalDetail;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.repository.CustomerRepository;
import com.campinggearrental.repository.EquipmentRepository;
import com.campinggearrental.repository.RentalOrderRepository;
import com.campinggearrental.state.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;

class RentalLifecycleServiceTest {
    @Test void confirmReservesOnceAndPreservesSnapshotPricingAndPayment() throws Exception {
        Fixture f = fixture(new PendingState(), 5, false, detail("EQ001", 2, "100.00"));
        f.service.confirmRental("RENT001");
        assertInstanceOf(ConfirmedState.class, f.order.getCurrentState()); assertEquals(3, f.equipment.getAvailableQuantity());
        assertEquals(new BigDecimal("100.00"), f.order.getDetails().getFirst().getUnitPrice());
        assertEquals(new BigDecimal("200.00"), f.order.getSubtotal()); assertEquals(PaymentStatus.UNPAID, f.order.getPaymentStatus());
        assertSame(f.tx.connection, f.equipmentRepository.connection); assertSame(f.tx.connection, f.orderRepository.connection);
        assertThrows(IllegalStateException.class, () -> f.service.confirmRental("RENT001")); assertEquals(3, f.equipment.getAvailableQuantity());
    }

    @Test void confirmValidatesEveryItemBeforeAnyReservation() {
        Fixture f = fixture(new PendingState(), 5, false, detail("EQ001", 2, "100.00"), detail("EQ002", 3, "40.00"));
        f.equipmentRepository.put(new Equipment("EQ002", "Bag", "CAT", new BigDecimal("40.00"), 2));
        assertThrows(IllegalStateException.class, () -> f.service.confirmRental("RENT001"));
        assertEquals(5, f.equipment.getAvailableQuantity()); assertInstanceOf(PendingState.class, f.order.getCurrentState());
        f.equipment.setStatus(EquipmentStatus.INACTIVE);
        assertThrows(IllegalStateException.class, () -> f.service.confirmRental("RENT001"));
        assertEquals(5, f.equipment.getAvailableQuantity());
    }

    @Test void rentCancelAndReturnApplyInventoryExactlyOnce() throws Exception {
        Fixture rent = fixture(new ConfirmedState(), 3, false, detail("EQ001", 2, "100.00"));
        rent.service.rentRental("RENT001"); assertInstanceOf(RentedState.class, rent.order.getCurrentState()); assertEquals(3, rent.equipment.getAvailableQuantity());
        assertThrows(IllegalStateException.class, () -> rent.service.rentRental("RENT001"));
        rent.service.returnRental("RENT001", LocalDate.of(2026, 10, 4));
        assertInstanceOf(ReturnedState.class, rent.order.getCurrentState()); assertEquals(5, rent.equipment.getAvailableQuantity());
        assertEquals(LocalDate.of(2026, 10, 4), rent.order.getActualReturnDate());
        assertThrows(IllegalStateException.class, () -> rent.service.returnRental("RENT001", LocalDate.of(2026, 10, 4))); assertEquals(5, rent.equipment.getAvailableQuantity());

        Fixture cancel = fixture(new ConfirmedState(), 3, false, detail("EQ001", 2, "100.00"));
        cancel.service.cancelRental("RENT001"); assertInstanceOf(CancelledState.class, cancel.order.getCurrentState()); assertEquals(5, cancel.equipment.getAvailableQuantity());
        assertThrows(IllegalStateException.class, () -> cancel.service.cancelRental("RENT001")); assertEquals(5, cancel.equipment.getAvailableQuantity());
    }

    @Test void pendingCancelAndInvalidReturnDatesDoNotChangeStock() throws Exception {
        Fixture pending = fixture(new PendingState(), 5, false, detail("EQ001", 2, "100.00"));
        pending.service.cancelRental("RENT001"); assertInstanceOf(CancelledState.class, pending.order.getCurrentState()); assertEquals(5, pending.equipment.getAvailableQuantity());
        Fixture rented = fixture(new RentedState(), 3, false, detail("EQ001", 2, "100.00"));
        assertThrows(IllegalArgumentException.class, () -> rented.service.returnRental("RENT001", null));
        assertThrows(IllegalArgumentException.class, () -> rented.service.returnRental("RENT001", LocalDate.of(2026, 9, 30)));
        assertEquals(3, rented.equipment.getAvailableQuantity());
    }

    @Test void persistenceFailureRollsBackAndRestoresInMemoryObjects() {
        Fixture f = fixture(new PendingState(), 5, true, detail("EQ001", 2, "100.00"));
        assertThrows(SQLException.class, () -> f.service.confirmRental("RENT001"));
        assertTrue(f.tx.rolledBack); assertEquals(5, f.equipment.getAvailableQuantity()); assertInstanceOf(PendingState.class, f.order.getCurrentState());
    }

    private static Fixture fixture(RentalState state, int available, boolean failOrderUpdate, RentalDetail... details) {
        Equipment equipment = new Equipment("EQ001", "Tent", "CAT", new BigDecimal("100.00"), 5, available, null);
        RentalOrder order = new RentalOrder(); order.setId("RENT001"); order.setRentalDate(LocalDate.of(2026, 10, 1)); order.setExpectedReturnDate(LocalDate.of(2026, 10, 3));
        order.setDetails(List.of(details)); order.setCurrentState(state); order.setSubtotal(new BigDecimal("200.00")); order.setDiscount(BigDecimal.ZERO); order.setTotal(new BigDecimal("200.00")); order.setPaymentStatus(PaymentStatus.UNPAID);
        Tx tx = new Tx(); EquipmentStore equipmentRepository = new EquipmentStore(equipment); Orders orderRepository = new Orders(order, failOrderUpdate);
        RentalService service = new RentalService(new Customers(), equipmentRepository, orderRepository, () -> tx.connection);
        return new Fixture(service, order, equipment, equipmentRepository, orderRepository, tx);
    }

    private static RentalDetail detail(String id, int quantity, String price) { RentalDetail d = new RentalDetail(); d.setEquipmentId(id); d.setQuantity(quantity); d.setUnitPrice(new BigDecimal(price)); return d; }
    private record Fixture(RentalService service, RentalOrder order, Equipment equipment, EquipmentStore equipmentRepository, Orders orderRepository, Tx tx) { }
    private static class Customers implements CustomerRepository { public List<Customer> findAll(){return List.of();} public List<Customer> search(String s){return List.of();} public Optional<Customer> findByPhone(String p){return Optional.empty();} public void insert(Customer c){} public void update(Customer c){} public void deleteById(String i){} }
    private static class EquipmentStore implements EquipmentRepository { final Map<String, Equipment> values=new HashMap<>(); Connection connection; EquipmentStore(Equipment e){put(e);} void put(Equipment e){values.put(e.getEquipmentId(),e);} public List<Equipment> findAll(){return List.copyOf(values.values());} public Optional<Equipment> findById(String id){return Optional.ofNullable(values.get(id));} public Optional<Equipment> findById(Connection c,String id){connection=c; return findById(id);} public List<Equipment> search(String s){return List.of();} public void insert(Equipment e){} public void update(Equipment e){} public void update(Connection c,Equipment e){connection=c;} }
    private static class Orders implements RentalOrderRepository { final RentalOrder order; final boolean fail; Connection connection; Orders(RentalOrder o,boolean f){order=o;fail=f;} public void insert(RentalOrder o){} public Optional<RentalOrder> findById(String id){return Optional.of(order);} public Optional<RentalOrder> findById(Connection c,String id){connection=c; return findById(id);} public List<RentalOrder> findAll(){return List.of(order);} public void update(RentalOrder o){} public void update(Connection c,RentalOrder o)throws SQLException{connection=c;if(fail)throw new SQLException("failure");} }
    private static class Tx { boolean autoCommit=true, rolledBack; final Connection connection=(Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{switch(m.getName()){case "getAutoCommit":return autoCommit;case "setAutoCommit":autoCommit=(boolean)a[0];return null;case "rollback":rolledBack=true;return null;case "commit","close":return null;case "isClosed":return false;case "unwrap":return null;case "isWrapperFor":return false;default: if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==int.class)return 0;if(m.getReturnType()==long.class)return 0L;return null;}}); }
}
