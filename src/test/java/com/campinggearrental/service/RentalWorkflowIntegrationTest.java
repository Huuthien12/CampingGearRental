package com.campinggearrental.service;

import static org.junit.jupiter.api.Assertions.*;
import com.campinggearrental.model.*;
import com.campinggearrental.repository.*;
import com.campinggearrental.state.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;

class RentalWorkflowIntegrationTest {
    @Test void completeWorkflowPreservesSnapshotAndInventoryInvariants() throws Exception {
        Fixture f = fixture(5, 5);
        RentalOrder order = f.rental.createDraft(request("EQ1", 2));
        assertInstanceOf(PendingState.class, order.getCurrentState()); assertEquals(5, f.tent.getAvailableQuantity()); assertEquals(new BigDecimal("100"), order.getDetails().getFirst().getUnitPrice());
        f.rental.confirmRental(order.getId()); assertInstanceOf(ConfirmedState.class, order.getCurrentState()); assertEquals(3, f.tent.getAvailableQuantity());
        assertThrows(IllegalStateException.class, () -> f.rental.confirmRental(order.getId())); assertEquals(3, f.tent.getAvailableQuantity());
        f.rental.rentRental(order.getId()); assertInstanceOf(RentedState.class, order.getCurrentState()); assertEquals(3, f.tent.getAvailableQuantity());
        f.tent.setPricePerDay(new BigDecimal("150")); f.checkout.refreshPricing(order.getId()); assertEquals(new BigDecimal("1000"), order.getSubtotal()); assertEquals(new BigDecimal("100.00"), order.getDiscount());
        f.checkout.confirmPayment(order.getId()); assertEquals(PaymentStatus.PAID, order.getPaymentStatus()); assertInstanceOf(RentedState.class, order.getCurrentState()); assertEquals(3, f.tent.getAvailableQuantity());
        f.rental.returnRental(order.getId(), LocalDate.of(2026, 2, 1)); assertInstanceOf(ReturnedState.class, order.getCurrentState()); assertEquals(5, f.tent.getAvailableQuantity());
        assertThrows(IllegalStateException.class, () -> f.rental.returnRental(order.getId(), LocalDate.now())); assertEquals(5, f.tent.getAvailableQuantity());
    }
    @Test void cancellationAndMultiItemInsufficientStockAreAtomic() throws Exception {
        Fixture f=fixture(5, 5); RentalOrder cancel=f.rental.createDraft(request("EQ1",2)); f.rental.confirmRental(cancel.getId()); f.rental.cancelRental(cancel.getId()); assertEquals(5,f.tent.getAvailableQuantity()); assertThrows(IllegalStateException.class,()->f.rental.cancelRental(cancel.getId()));
        RentalOrder pending=f.rental.createDraft(request("EQ1",2)); f.rental.cancelRental(pending.getId()); assertEquals(5,f.tent.getAvailableQuantity());
        RentalOrder multi=f.rental.createDraft(new RentalService.RentalRequest("C1",LocalDate.of(2026,1,1),LocalDate.of(2026,1,3),List.of(new RentalService.RentalRequestItem("EQ1",2),new RentalService.RentalRequestItem("EQ2",4)))); f.chair.reserve(2); assertThrows(IllegalStateException.class,()->f.rental.confirmRental(multi.getId())); assertInstanceOf(PendingState.class,multi.getCurrentState()); assertEquals(5,f.tent.getAvailableQuantity()); assertEquals(3,f.chair.getAvailableQuantity());
    }
    private static RentalService.RentalRequest request(String id,int q){return new RentalService.RentalRequest("C1",LocalDate.of(2026,1,1),LocalDate.of(2026,1,6),List.of(new RentalService.RentalRequestItem(id,q)));}
    private static Fixture fixture(int tentAvailable,int chairAvailable){ EquipmentStore e=new EquipmentStore(); Orders o=new Orders(); Equipment tent=new Equipment("EQ1","Tent","C",new BigDecimal("100"),5,tentAvailable,EquipmentStatus.AVAILABLE),chair=new Equipment("EQ2","Chair","C",new BigDecimal("30"),5,chairAvailable,EquipmentStatus.AVAILABLE); e.values.put("EQ1",tent);e.values.put("EQ2",chair); Connection c=(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{if(m.getName().equals("getAutoCommit"))return true;if(m.getName().equals("isClosed")||m.getName().equals("isWrapperFor"))return false;if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==int.class)return 0;return null;}); RentalService r=new RentalService(new Customers(),e,o,()->c); PersistedCheckoutService co=new PersistedCheckoutService(o,new CheckoutService(new RentalPricingAdapter(new PricingService())));return new Fixture(r,co,tent,chair);}
    private record Fixture(RentalService rental,PersistedCheckoutService checkout,Equipment tent,Equipment chair){}
    private static class Customers implements CustomerRepository { public List<Customer> findAll(){return List.of(new Customer("C1","C","1","",""));}public List<Customer> search(String x){return List.of();}public Optional<Customer> findByPhone(String x){return Optional.empty();}public void insert(Customer x){}public void update(Customer x){}public void deleteById(String x){} }
    private static class EquipmentStore implements EquipmentRepository { final Map<String,Equipment> values=new HashMap<>();public List<Equipment> findAll(){return List.copyOf(values.values());}public Optional<Equipment> findById(String x){return Optional.ofNullable(values.get(x));}public Optional<Equipment> findById(Connection c,String x){return findById(x);}public Optional<Equipment> findByIdForUpdate(Connection c,String x){return findById(x);}public List<Equipment> search(String x){return List.of();}public void insert(Equipment x){}public void update(Equipment x){}public void update(Connection c,Equipment x){} }
    private static class Orders implements RentalOrderRepository { final Map<String,RentalOrder> values=new HashMap<>();public void insert(RentalOrder x){values.put(x.getId(),x);}public Optional<RentalOrder> findById(String x){return Optional.ofNullable(values.get(x));}public Optional<RentalOrder> findById(Connection c,String x){return findById(x);}public List<RentalOrder> findAll(){return List.copyOf(values.values());}public void update(RentalOrder x){values.put(x.getId(),x);}public void update(Connection c,RentalOrder x){update(x);} }
}
