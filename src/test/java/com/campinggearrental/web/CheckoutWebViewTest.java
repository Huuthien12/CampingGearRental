package com.campinggearrental.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalDetail;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.repository.RentalOrderRepository;
import com.campinggearrental.service.CheckoutService;
import com.campinggearrental.service.PersistedCheckoutService;
import com.campinggearrental.service.PricingService;
import com.campinggearrental.service.RentalPricingAdapter;
import com.campinggearrental.state.RentedState;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class CheckoutWebViewTest {
    private static ServletWebServerApplicationContext context;
    private static String base;

    @BeforeAll static void start() {
        SpringApplication app = new SpringApplication(TestConfiguration.class);
        app.setDefaultProperties(java.util.Map.of("server.port", "0", "spring.main.banner-mode", "off"));
        context = (ServletWebServerApplicationContext) app.run();
        base = "http://localhost:" + context.getWebServer().getPort();
    }

    @AfterAll static void stop() { if (context != null) context.close(); }

    @Test void rentedCheckoutDetailRenders() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/checkout/RENT001"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("RENT001"), response.body());
        assertTrue(response.body().contains("RENTED"), response.body());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({CheckoutWebController.class, WebErrorHandler.class})
    static class TestConfiguration {
        @Bean PricingService pricingService() { return new PricingService(); }
        @Bean PersistedCheckoutService persistedCheckoutService(PricingService pricingService) {
            return new FixedCheckout(pricingService);
        }
    }

    static class FixedCheckout extends PersistedCheckoutService {
        FixedCheckout(PricingService pricingService) {
            super(new RentalOrderRepository() {
                public void insert(RentalOrder order) { }
                public Optional<RentalOrder> findById(String id) { return Optional.empty(); }
                public List<RentalOrder> findAll() { return List.of(); }
                public void update(RentalOrder order) { }
            }, new CheckoutService(new RentalPricingAdapter(pricingService)));
        }

        @Override public RentalOrder load(String id) { return order(id); }

        private static RentalOrder order(String id) {
            RentalDetail detail = new RentalDetail();
            detail.setEquipmentId("EQ001"); detail.setQuantity(1); detail.setUnitPrice(new BigDecimal("100000.00"));
            RentalOrder order = new RentalOrder();
            order.setId(id); order.setCustomerId("CUS001"); order.setRentalDate(LocalDate.of(2026, 10, 8));
            order.setExpectedReturnDate(LocalDate.of(2026, 10, 13)); order.setCurrentState(new RentedState());
            order.setPaymentStatus(PaymentStatus.UNPAID); order.setDetails(List.of(detail));
            return order;
        }
    }
}
