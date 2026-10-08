package com.campinggearrental.web;

import static org.junit.jupiter.api.Assertions.*;

import com.campinggearrental.model.Category;
import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.repository.CategoryRepository;
import com.campinggearrental.repository.EquipmentRepository;
import com.campinggearrental.service.CategoryService;
import com.campinggearrental.service.EquipmentService;
import java.math.BigDecimal;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Real HTTP/MVC/Thymeleaf tests using existing services and isolated in-memory repositories. */
class EquipmentWebTest {
    private static ServletWebServerApplicationContext context;
    private static MemoryEquipment repository;
    private static String base;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll static void start() {
        SpringApplication app = new SpringApplication(TestConfiguration.class);
        app.setDefaultProperties(Map.of("server.port", "0", "spring.main.banner-mode", "off"));
        context = (ServletWebServerApplicationContext) app.run();
        repository = context.getBean(MemoryEquipment.class);
        base = "http://localhost:" + context.getWebServer().getPort();
    }

    @AfterAll static void stop() { if (context != null) context.close(); }

    @BeforeEach void seed() throws SQLException {
        repository.values.clear();
        repository.failWrites = false;
        repository.lastSearch = null;
        repository.insert(new Equipment("EQ001", "Tent", "CAT001", new BigDecimal("100.00"), 5, 3, EquipmentStatus.AVAILABLE));
    }

    @Test void listRendersCategoryPriceStockStatusAndEditAction() throws Exception {
        HttpResponse<String> response = get("/equipment");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Lều"));
        assertTrue(response.body().contains("100.00"));
        assertTrue(response.body().contains("3 / 5"));
        assertTrue(response.body().contains("Sẵn sàng cho thuê"));
        assertTrue(response.body().contains("/equipment/EQ001/edit"));
    }

    @Test void searchDelegatesToExistingContract() throws Exception {
        assertTrue(get("/equipment?q=Tent").body().contains("EQ001"));
        assertEquals("Tent", repository.lastSearch);
        assertTrue(get("/equipment?q=CAT001").body().contains("EQ001"));
        assertTrue(get("/equipment?q=missing").body().contains("Chưa có thiết bị phù hợp"));
    }

    @Test void newFormLoadsCategoriesAndValidCreateRedirectsWithFeedback() throws Exception {
        String html = get("/equipment/new").body();
        assertTrue(html.contains("Lều (CAT001)"));
        assertTrue(html.contains("value=\"CAT001\"") && html.contains("value=\"AVAILABLE\""));
        assertFalse(html.contains("name=\"availableQuantity\""));
        HttpResponse<String> response = post("/equipment", valid("EQ002"));
        assertEquals(302, response.statusCode(), response.body());
        String location = response.headers().firstValue("location").orElseThrow();
        assertEquals("/equipment", URI.create(location).getPath().split(";", 2)[0]);
        Equipment created = repository.values.get("EQ002");
        assertEquals(4, created.getAvailableQuantity());
        assertEquals(4, created.getTotalQuantity());
        assertEquals("CAT001", created.getCategoryId());
        String cookie = response.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
        HttpResponse<String> list = client.send(HttpRequest.newBuilder(URI.create(base + "/equipment"))
                .header("Cookie", cookie).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertTrue(list.body().contains("Đã thêm thiết bị."));
    }

    @Test void invalidPriceIsRejectedWithoutPersistence() throws Exception {
        HttpResponse<String> response = post("/equipment", valid("EQ002").replace("pricePerDay=120", "pricePerDay=0"));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Vui lòng kiểm tra lại biểu mẫu"));
        assertFalse(repository.values.containsKey("EQ002"));
    }

    @Test void invalidQuantityAndNumericBindingAreRejected() throws Exception {
        assertTrue(post("/equipment", valid("EQ002").replace("totalQuantity=4", "totalQuantity=-1")).body().contains("Vui lòng kiểm tra lại biểu mẫu"));
        assertTrue(post("/equipment", valid("EQ002").replace("totalQuantity=4", "totalQuantity=abc")).body().contains("Vui lòng kiểm tra lại biểu mẫu"));
        assertFalse(repository.values.containsKey("EQ002"));
    }

    @Test void unknownCategoryIsRejectedAndFormRetainsInput() throws Exception {
        String html = post("/equipment", valid("EQ002").replace("categoryId=CAT001", "categoryId=missing")).body();
        assertTrue(html.contains("Hãy chọn một loại thiết bị có sẵn."));
        assertTrue(html.contains("New tent"));
        assertFalse(repository.values.containsKey("EQ002"));
    }

    @Test void availableGreaterThanTotalInputIsRejected() throws Exception {
        String html = post("/equipment", valid("EQ002") + "&availableQuantity=999").body();
        assertTrue(html.contains("Không thể chỉnh sửa trực tiếp số lượng sẵn sàng."));
        assertFalse(repository.values.containsKey("EQ002"));
        assertThrows(IllegalArgumentException.class, () -> new Equipment("E", "Tent", "CAT001", BigDecimal.ONE,
                4, 5, EquipmentStatus.AVAILABLE));
    }

    @Test void editLoadsAndUpdatesWithoutResettingReservedQuantity() throws Exception {
        String html = get("/equipment/EQ001/edit").body();
        assertTrue(html.contains("3 / 5"));
        assertTrue(html.contains("Lều (CAT001)"));
        assertEquals(302, post("/equipment/EQ001/edit", valid("tampered-id")
                .replace("totalQuantity=4", "totalQuantity=7").replace("status=AVAILABLE", "status=INACTIVE")).statusCode());
        Equipment updated = repository.values.get("EQ001");
        assertEquals("New tent", updated.getName());
        assertEquals(new BigDecimal("120"), updated.getPricePerDay());
        assertEquals(7, updated.getTotalQuantity());
        assertEquals(5, updated.getAvailableQuantity());
        assertEquals(EquipmentStatus.INACTIVE, updated.getStatus());
        assertFalse(repository.values.containsKey("tampered-id"));
    }

    @Test void editBelowReservedQuantityDoesNotMutateExistingData() throws Exception {
        String html = post("/equipment/EQ001/edit", valid("EQ001").replace("totalQuantity=4", "totalQuantity=1")).body();
        assertTrue(html.contains("Vui lòng kiểm tra lại biểu mẫu"));
        assertEquals("Tent", repository.values.get("EQ001").getName());
        assertEquals(5, repository.values.get("EQ001").getTotalQuantity());
        assertEquals(3, repository.values.get("EQ001").getAvailableQuantity());
    }

    @Test void editRejectsDirectAvailableQuantityAndInvalidStatus() throws Exception {
        assertTrue(post("/equipment/EQ001/edit", valid("EQ001") + "&availableQuantity=9").body()
                .contains("Không thể chỉnh sửa trực tiếp số lượng sẵn sàng."));
        assertTrue(post("/equipment/EQ001/edit", valid("EQ001").replace("status=AVAILABLE", "status=UNKNOWN")).body()
                .contains("Vui lòng kiểm tra lại biểu mẫu"));
        assertEquals(3, repository.values.get("EQ001").getAvailableQuantity());
    }

    @Test void databaseErrorDoesNotExposeSqlOrCredentials() throws Exception {
        repository.failWrites = true;
        String html = post("/equipment", valid("EQ002")).body();
        assertTrue(html.contains("Không thể lưu thiết bị."));
        assertFalse(html.contains("SECRET_DB_DETAIL"));
        assertFalse(repository.values.containsKey("EQ002"));
    }

    @Test void missingEquipmentUsesExistingSafeErrorPage() throws Exception {
        assertTrue(get("/equipment/missing/edit").body().contains("The request could not be completed."));
    }

    @Test void inactiveEditRequiresStatusAndPreservesDataOnMissingOrEmptyStatus() throws Exception {
        Equipment current = repository.values.get("EQ001");
        current.setStatus(EquipmentStatus.INACTIVE);
        for (String body : List.of(valid("EQ001").replace("&status=AVAILABLE", ""),
                valid("EQ001").replace("status=AVAILABLE", "status="))) {
            HttpResponse<String> response = post("/equipment/EQ001/edit", body);
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("Vui lòng kiểm tra lại biểu mẫu"));
            assertSame(current, repository.values.get("EQ001"));
            assertEquals(EquipmentStatus.INACTIVE, current.getStatus());
            assertEquals("Tent", current.getName());
            assertEquals(3, current.getAvailableQuantity());
        }
    }

    @Test void inactiveEditAcceptsExplicitValidStatus() throws Exception {
        repository.values.get("EQ001").setStatus(EquipmentStatus.INACTIVE);
        assertEquals(302, post("/equipment/EQ001/edit", valid("EQ001")).statusCode());
        assertEquals(EquipmentStatus.AVAILABLE, repository.values.get("EQ001").getStatus());
        assertEquals(2, repository.values.get("EQ001").getAvailableQuantity());
    }

    @Test void addStillDefaultsStatusWhenPostOmitsIt() throws Exception {
        assertEquals(302, post("/equipment", valid("EQ002").replace("&status=AVAILABLE", "")).statusCode());
        assertEquals(EquipmentStatus.AVAILABLE, repository.values.get("EQ002").getStatus());
        assertEquals(4, repository.values.get("EQ002").getAvailableQuantity());
    }

    private String valid(String id) {
        return "equipmentId=" + id + "&name=New+tent&categoryId=CAT001&pricePerDay=120&totalQuantity=4&status=AVAILABLE";
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({EquipmentWebController.class, PresentationLabels.class, WebErrorHandler.class})
    static class TestConfiguration {
        @Bean MemoryEquipment equipmentRepository() { return new MemoryEquipment(); }
        @Bean EquipmentService equipmentService(MemoryEquipment repository) {
            return new EquipmentService(repository, EquipmentWebTest::transactionConnection);
        }
        @Bean CategoryService categoryService() {
            return new CategoryService(new CategoryRepository() {
                public List<Category> findAll() { return List.of(new Category("CAT001", "Tent")); }
                public Optional<Category> findById(String id) { return findAll().stream().filter(c -> c.getCategoryId().equals(id)).findFirst(); }
            });
        }
    }

    static class MemoryEquipment implements EquipmentRepository {
        final Map<String, Equipment> values = new LinkedHashMap<>();
        boolean failWrites;
        String lastSearch;
        public List<Equipment> findAll() { return List.copyOf(values.values()); }
        public Optional<Equipment> findById(String id) { return Optional.ofNullable(values.get(id)); }
        public Optional<Equipment> findByIdForUpdate(Connection connection, String id) { return findById(id); }
        public List<Equipment> search(String keyword) {
            lastSearch = keyword;
            return values.values().stream().filter(e -> e.getName().contains(keyword) || e.getCategoryId().contains(keyword)).toList();
        }
        public void insert(Equipment equipment) throws SQLException {
            if (failWrites) throw new SQLException("SECRET_DB_DETAIL");
            if (values.containsKey(equipment.getEquipmentId())) throw new IllegalArgumentException("Duplicate ID");
            values.put(equipment.getEquipmentId(), equipment);
        }
        public void update(Equipment equipment) throws SQLException {
            if (failWrites) throw new SQLException("SECRET_DB_DETAIL");
            values.put(equipment.getEquipmentId(), equipment);
        }
        public void update(Connection connection, Equipment equipment) throws SQLException { update(equipment); }
    }

    private static Connection transactionConnection() {
        boolean[] autoCommit = {true};
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getAutoCommit" -> autoCommit[0];
                    case "setAutoCommit" -> { autoCommit[0] = (boolean) arguments[0]; yield null; }
                    case "commit", "rollback", "close" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
