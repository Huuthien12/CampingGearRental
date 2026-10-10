package com.campinggearrental.service;

import static org.junit.jupiter.api.Assertions.*;

import com.campinggearrental.repository.JdbcEquipmentRepository;
import com.campinggearrental.repository.JdbcRentalOrderRepository;
import com.campinggearrental.repository.RentalOrderRepository;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

/** Runs only through CAMPING_TEST_DB_*; it rejects every database other than the disposable test schema. */
@Tag("mysql-integration")
class RentalLifecycleMySqlIntegrationTest {
    private final String token = "F61C" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private String url, user, password;

    @BeforeEach void connectOnlyToTestSchema() throws Exception {
        url = required("CAMPING_TEST_DB_URL"); user = required("CAMPING_TEST_DB_USERNAME"); password = required("CAMPING_TEST_DB_PASSWORD");
        assertTrue(isTestJdbcUrl(url), "refusing non-test JDBC URL");
        assertEquals("camping_test_runner", user, "refusing non-test user");
        try (Connection c = connection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT DATABASE(), CURRENT_USER()")) {
            assertTrue(r.next()); assertEquals("camping_gear_rental_test", r.getString(1)); assertTrue(r.getString(2).startsWith("camping_test_runner@"));
        }
    }

    @AfterEach void cleanup() throws Exception {
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            for (String sql : List.of("DELETE FROM rental_details WHERE rental_order_id LIKE ?", "DELETE FROM rental_orders WHERE id LIKE ?", "DELETE FROM equipment WHERE id LIKE ?", "DELETE FROM customers WHERE id LIKE ?", "DELETE FROM categories WHERE id LIKE ?")) {
                try (PreparedStatement p = c.prepareStatement(sql)) { p.setString(1, token + "%"); p.executeUpdate(); }
            }
            c.commit();
        }
    }

    @Test void tc01_competingOrdersNeverOversell() throws Exception {
        String equipment = equipment(5); String a = order("PENDING", equipment, 3); String b = order("PENDING", equipment, 4);
        List<Boolean> results = concurrent(() -> confirm(a), () -> confirm(b));
        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        String aState = state(a), bState = state(b);
        assertEquals(1, List.of(aState, bState).stream().filter("CONFIRMED"::equals).count());
        assertEquals(1, List.of(aState, bState).stream().filter("PENDING"::equals).count());
        assertEquals(results.get(0), "CONFIRMED".equals(aState)); assertEquals(results.get(1), "CONFIRMED".equals(bState));
        assertTrue(stock(equipment) == 2 || stock(equipment) == 1); assertTrue(stock(equipment) >= 0);
    }

    @Test void tc02_sameOrderIsConfirmedOnce() throws Exception {
        String equipment = equipment(5); String order = order("PENDING", equipment, 3);
        assertEquals(1, concurrent(() -> confirm(order), () -> confirm(order)).stream().filter(Boolean::booleanValue).count());
        assertEquals("CONFIRMED", state(order)); assertEquals(2, stock(equipment));
    }

    @Test void tc03_confirmAndCancelSerializeWithoutLeakingStock() throws Exception {
        String equipment = equipment(5); String order = order("PENDING", equipment, 3);
        concurrent(() -> confirm(order), () -> cancel(order));
        assertEquals("CANCELLED", state(order)); assertEquals(5, stock(equipment));
    }

    @Test void tc04_returnReleasesStockOnce() throws Exception {
        String equipment = equipment(5); String order = order("RENTED", equipment, 3); setStock(equipment, 2);
        assertEquals(1, concurrent(() -> returned(order), () -> returned(order)).stream().filter(Boolean::booleanValue).count());
        assertEquals("RETURNED", state(order)); assertEquals(5, stock(equipment));
    }

    @Test void tc05_rollbackLeavesPersistedInventoryUntouched() throws Exception {
        String equipment = equipment(5), order = order("PENDING", equipment, 3);
        FailingOrderRepository repository = new FailingOrderRepository();
        assertThrows(SQLException.class, () -> service(repository).confirmRental(order));
        assertTrue(repository.updateAttempted, "controlled failure must occur after inventory persistence");
        assertEquals("PENDING", state(order)); assertEquals(5, stock(equipment));
    }

    @Test void tc06_multiEquipmentOrdersUseConsistentLocks() throws Exception {
        String one = equipment(5), two = equipment(5); String a = order("PENDING", List.of(one, two), List.of(1, 1)); String b = order("PENDING", List.of(two, one), List.of(1, 1));
        assertEquals(List.of(true, true), concurrent(() -> confirm(a), () -> confirm(b))); assertEquals(3, stock(one)); assertEquals(3, stock(two));
    }

    private boolean confirm(String id) { try { service().confirmRental(id); return true; } catch (IllegalStateException e) { return false; } catch (SQLException e) { throw new RuntimeException(e); } }
    private boolean cancel(String id) { try { service().cancelRental(id); return true; } catch (IllegalStateException e) { return false; } catch (SQLException e) { throw new RuntimeException(e); } }
    private boolean returned(String id) { try { service().returnRental(id, LocalDate.of(2026, 10, 10)); return true; } catch (IllegalStateException e) { return false; } catch (SQLException e) { throw new RuntimeException(e); } }
    private RentalService service() { return service(new JdbcRentalOrderRepository()); }
    private RentalService service(RentalOrderRepository orders) { return new RentalService(new TestCustomers(), new JdbcEquipmentRepository(), orders, this::connection); }

    private String equipment(int total) throws Exception { String id = token + "E" + UUID.randomUUID().toString().substring(0, 5), category = token + "C" + UUID.randomUUID().toString().substring(0, 5); try (Connection c = connection(); PreparedStatement p = c.prepareStatement("INSERT INTO categories(id,name) VALUES(?,?)")) { p.setString(1, category); p.setString(2, id); p.executeUpdate(); try (PreparedStatement e = c.prepareStatement("INSERT INTO equipment(id,name,category_id,price_per_day,total_quantity,available_quantity,status) VALUES(?,?,?,10,?,?, 'AVAILABLE')")) { e.setString(1,id); e.setString(2,id); e.setString(3,category); e.setInt(4,total); e.setInt(5,total); e.executeUpdate(); } } return id; }
    private String order(String status, String equipment, int quantity) throws Exception { return order(status, List.of(equipment), List.of(quantity)); }
    private String order(String status, List<String> equipment, List<Integer> quantities) throws Exception { String customer = token + "U" + UUID.randomUUID().toString().substring(0, 5), id = token + "O" + UUID.randomUUID().toString().substring(0, 5); try (Connection c=connection()) { try (PreparedStatement p=c.prepareStatement("INSERT INTO customers(id,full_name,phone) VALUES(?,?,?)")) {p.setString(1,customer);p.setString(2,customer);p.setString(3,customer);p.executeUpdate();} try (PreparedStatement p=c.prepareStatement("INSERT INTO rental_orders(id,customer_id,rental_date,expected_return_date,status,subtotal,discount,total,payment_status) VALUES(?,?,CURDATE(),DATE_ADD(CURDATE(),INTERVAL 1 DAY),?,0,0,0,'UNPAID')")) {p.setString(1,id);p.setString(2,customer);p.setString(3,status);p.executeUpdate();} for(int i=0;i<equipment.size();i++) try(PreparedStatement p=c.prepareStatement("INSERT INTO rental_details(rental_order_id,equipment_id,quantity,unit_price) VALUES(?,?,?,10)")){p.setString(1,id);p.setString(2,equipment.get(i));p.setInt(3,quantities.get(i));p.executeUpdate();} } return id; }
    private int stock(String id) throws Exception { try(Connection c=connection(); PreparedStatement p=c.prepareStatement("SELECT available_quantity FROM equipment WHERE id=?")){p.setString(1,id);try(ResultSet r=p.executeQuery()){assertTrue(r.next());return r.getInt(1);}} }
    private void setStock(String id,int value) throws Exception {try(Connection c=connection();PreparedStatement p=c.prepareStatement("UPDATE equipment SET available_quantity=? WHERE id=?")){p.setInt(1,value);p.setString(2,id);p.executeUpdate();}}
    private String state(String id) throws Exception {try(Connection c=connection();PreparedStatement p=c.prepareStatement("SELECT status FROM rental_orders WHERE id=?")){p.setString(1,id);try(ResultSet r=p.executeQuery()){assertTrue(r.next());return r.getString(1);}}}
    private List<Boolean> concurrent(Callable<Boolean> a, Callable<Boolean> b) throws Exception { CountDownLatch ready=new CountDownLatch(2), go=new CountDownLatch(1); ExecutorService pool=Executors.newFixedThreadPool(2); try { Future<Boolean> x=pool.submit(gate(a,ready,go)), y=pool.submit(gate(b,ready,go)); assertTrue(ready.await(5,TimeUnit.SECONDS)); go.countDown(); return List.of(x.get(15,TimeUnit.SECONDS),y.get(15,TimeUnit.SECONDS)); } finally {pool.shutdownNow();} }
    private static Callable<Boolean> gate(Callable<Boolean> action, CountDownLatch ready, CountDownLatch go) { return () -> {ready.countDown(); if(!go.await(5,TimeUnit.SECONDS)) throw new TimeoutException(); return action.call();}; }
    private Connection connection() throws SQLException { return DriverManager.getConnection(url,user,password); }
    static boolean isTestJdbcUrl(String value) { return value != null && value.matches("(?i)^jdbc:mysql://[^/]+/camping_gear_rental_test(?:$|[?;].*)"); }
    private static String required(String name) { String v=System.getenv(name); if(v==null||v.isBlank()) throw new IllegalStateException(name+" must be set"); return v; }
    private static class FailingOrderRepository extends JdbcRentalOrderRepository {
        boolean updateAttempted;
        @Override public void update(Connection connection, com.campinggearrental.model.RentalOrder order) throws SQLException { updateAttempted = true; throw new SQLException("controlled lifecycle update failure"); }
    }
    private static class TestCustomers implements com.campinggearrental.repository.CustomerRepository { public List<com.campinggearrental.model.Customer> findAll(){return List.of();} public Optional<com.campinggearrental.model.Customer> findById(String x){return Optional.empty();} public List<com.campinggearrental.model.Customer> search(String x){return List.of();} public Optional<com.campinggearrental.model.Customer> findByPhone(String x){return Optional.empty();} public void insert(com.campinggearrental.model.Customer x){} public void update(com.campinggearrental.model.Customer x){} public void deleteById(String x){} }
}
