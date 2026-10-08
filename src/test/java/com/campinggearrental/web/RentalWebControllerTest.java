package com.campinggearrental.web;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.campinggearrental.factory.CampingPackageType;
import com.campinggearrental.factory.CampingPackageFactory;
import com.campinggearrental.model.Category;
import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalDetail;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.service.CampingPackageDraftService;
import com.campinggearrental.service.RentalOrderService;
import com.campinggearrental.service.RentalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(RentalWebController.class)
@Import({PresentationLabels.class, RentalWebControllerTest.AuthenticatedRoutesConfiguration.class})
class RentalWebControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private CampingPackageDraftService campingPackageDraftService;
    @MockBean private RentalOrderService rentalOrderService;
    @MockBean private RentalService rentalService;
    @MockBean private com.campinggearrental.service.EquipmentService equipmentService;
    @MockBean private com.campinggearrental.service.CategoryService categoryService;

    @Test void listsPersistedRentals() throws Exception {
        RentalOrder order = order("RENT001");
        when(rentalOrderService.findAll()).thenReturn(List.of(order));
        mockMvc.perform(get("/rentals").session(authenticatedSession())).andExpect(status().isOk()).andExpect(view().name("rentals/list"))
                .andExpect(model().attribute("rentals", List.of(order)));
        verify(rentalOrderService).findAll();
    }

    @Test void displaysPersistedDetailWithHistoricalPrice() throws Exception {
        RentalOrder order = order("RENT001");
        when(rentalOrderService.findById("RENT001")).thenReturn(order);
        mockMvc.perform(get("/rentals/RENT001").session(authenticatedSession())).andExpect(status().isOk()).andExpect(view().name("rentals/detail"))
                .andExpect(model().attribute("order", order));
        verify(rentalOrderService).findById("RENT001");
    }

    @Test void returnsNotFoundViewForUnknownRental() throws Exception {
        when(rentalOrderService.findById("MISSING")).thenReturn(null);
        mockMvc.perform(get("/rentals/MISSING").session(authenticatedSession())).andExpect(status().isOk()).andExpect(view().name("error/404"));
    }

    @Test void opensDraftFormWithSupportedPackageTypes() throws Exception {
        when(equipmentService.list()).thenReturn(List.of(equipment("EQ001"),
                new Equipment("EQ002", "Túi ngủ", "CAT001", new BigDecimal("100.00"), 5, 5, EquipmentStatus.AVAILABLE)));
        mockMvc.perform(get("/rentals/new").session(authenticatedSession())).andExpect(status().isOk()).andExpect(view().name("rentals/new"))
                .andExpect(model().attributeExists("rentalDraftForm", "packageTypes", "packageItems"))
                .andExpect(result -> {
                    Object modelValue = result.getModelAndView().getModel().get("packageItems");
                    assertTrue(modelValue instanceof Map<?, ?>);
                    Map<?, ?> packageItems = (Map<?, ?>) modelValue;
                    for (CampingPackageType type : CampingPackageType.values()) {
                        List<?> views = (List<?>) packageItems.get(type);
                        assertEquals(CampingPackageFactory.create(type).items().size(), views.size());
                        for (int index = 0; index < views.size(); index++) {
                            RentalWebController.PackageItemView view = (RentalWebController.PackageItemView) views.get(index);
                            assertEquals(CampingPackageFactory.create(type).items().get(index).equipmentId(), view.equipmentId());
                            assertEquals(CampingPackageFactory.create(type).items().get(index).quantity(), view.quantity());
                        }
                    }
                    List<?> soloItems = (List<?>) packageItems.get(CampingPackageType.SOLO);
                    assertEquals("Tent", ((RentalWebController.PackageItemView) soloItems.get(0)).name());
                    RentalWebController.PackageItemView missing = (RentalWebController.PackageItemView) soloItems.get(2);
                    assertEquals(missing.equipmentId(), missing.name());
                });
        verify(equipmentService).list();
    }

    @Test void acceptsExactBrowserFormRequestForServerDefinedPackage() throws Exception {
        RentalOrder order = order("RENT001");
        when(campingPackageDraftService.createDraftFromPackage(eq("CUS001"), eq(LocalDate.of(2026, 10, 8)),
                eq(LocalDate.of(2026, 10, 13)), eq(CampingPackageType.SOLO))).thenReturn(order);
        mockMvc.perform(post("/rentals").contentType(MediaType.APPLICATION_FORM_URLENCODED).param("customerId", "CUS001").param("rentalDate", "2026-10-08")
                        .param("expectedReturnDate", "2026-10-13").param("packageType", "SOLO").session(authenticatedSession()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/rentals/RENT001"));
        verify(campingPackageDraftService).createDraftFromPackage("CUS001", LocalDate.of(2026, 10, 8),
                LocalDate.of(2026, 10, 13), CampingPackageType.SOLO);
    }

    @Test void keepsSubmittedValuesForInvalidDates() throws Exception {
        mockMvc.perform(post("/rentals").param("customerId", "CUS001").param("rentalDate", "2026-10-07")
                        .param("expectedReturnDate", "2026-10-05").param("packageType", "SOLO").session(authenticatedSession()))
                .andExpect(status().isOk()).andExpect(view().name("rentals/new"))
                .andExpect(model().attributeHasFieldErrors("rentalDraftForm", "expectedReturnDate"))
                .andExpect(model().attribute("rentalDraftForm", allOf(hasProperty("customerId", is("CUS001")),
                        hasProperty("packageType", is(CampingPackageType.SOLO)))));
        verifyNoInteractions(campingPackageDraftService);
    }

    @Test void rejectsInvalidCustomerPackageAndPackageItemOverride() throws Exception {
        mockMvc.perform(post("/rentals").param("customerId", "").param("rentalDate", "2026-10-05")
                        .param("expectedReturnDate", "2026-10-07").param("packageType", "UNKNOWN")
                        .param("equipmentId", "EQ999").param("quantity", "999").session(authenticatedSession()))
                .andExpect(status().isOk()).andExpect(view().name("rentals/new"))
                .andExpect(model().attributeHasFieldErrors("rentalDraftForm", "customerId", "packageType"));
        verifyNoInteractions(campingPackageDraftService);
    }

    @Test void opensAuthenticatedCustomDraftFormWithEquipmentOptions() throws Exception {
        when(equipmentService.list()).thenReturn(List.of(equipment("EQ001")));
        when(categoryService.list()).thenReturn(List.of(new Category("CAT001", "Lều cắm trại")));
        mockMvc.perform(get("/rentals/custom/new").session(authenticatedSession()))
                .andExpect(status().isOk()).andExpect(view().name("rentals/custom-new"))
                .andExpect(model().attributeExists("customRentalDraftForm", "equipmentOptions", "categories"));
    }

    @Test void customRentalTemplateUsesDataDrivenSelectionAssetsAndResponsiveStyles() throws Exception {
        String template = new String(getClass().getResourceAsStream("/templates/rentals/custom-new.html").readAllBytes());
        String script = new String(getClass().getResourceAsStream("/static/js/custom-rental.js").readAllBytes());
        String css = new String(getClass().getResourceAsStream("/static/css/custom-rental.css").readAllBytes());
        assertTrue(template.contains("equipmentOptions") && template.contains("categories"));
        assertTrue(template.contains("/css/custom-rental.css") && template.contains("/js/custom-rental.js"));
        assertTrue(script.contains("items[${index}].equipmentId") && script.contains("selected.delete"));
        assertTrue(css.contains("@media (max-width: 1199px)") && css.contains("@media (max-width: 767px)"));
    }

    @Test void customRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/rentals/custom/new")).andExpect(redirectedUrl("/login"));
        mockMvc.perform(post("/rentals/custom")).andExpect(redirectedUrl("/login"));
        verifyNoInteractions(equipmentService, rentalService);
    }

    @Test void createsValidMultiItemCustomDraftFromBrowserForm() throws Exception {
        RentalOrder order = order("RENT-CUSTOM");
        RentalService.RentalRequest request = new RentalService.RentalRequest("CUS001", LocalDate.of(2026, 10, 8),
                LocalDate.of(2026, 10, 13), List.of(new RentalService.RentalRequestItem("EQ001", 1),
                        new RentalService.RentalRequestItem("EQ002", 2)));
        when(rentalService.createDraft(eq(request))).thenReturn(order);
        mockMvc.perform(post("/rentals/custom").contentType(MediaType.APPLICATION_FORM_URLENCODED).session(authenticatedSession())
                        .param("customerId", "CUS001").param("rentalDate", "2026-10-08").param("expectedReturnDate", "2026-10-13")
                        .param("items[0].equipmentId", "EQ001").param("items[0].quantity", "1")
                        .param("items[1].equipmentId", "EQ002").param("items[1].quantity", "2"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/rentals/RENT-CUSTOM"));
        verify(rentalService).createDraft(request);
    }

    @Test void createsCustomDraftWithMaximumTenRows() throws Exception {
        List<RentalService.RentalRequestItem> items = new ArrayList<>();
        for (int index = 0; index < 10; index++) items.add(new RentalService.RentalRequestItem("EQ" + index, index + 1));
        RentalService.RentalRequest request = new RentalService.RentalRequest("CUS001", LocalDate.of(2026, 10, 8),
                LocalDate.of(2026, 10, 13), items);
        RentalOrder order = order("RENT-TEN");
        when(rentalService.createDraft(eq(request))).thenReturn(order);
        mockMvc.perform(customPostWithRows(10)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/rentals/RENT-TEN"));
        verify(rentalService).createDraft(request);
    }

    @Test void rejectsEmptyOrIncompleteCustomItemsWithoutDelegating() throws Exception {
        mockMvc.perform(customPost().param("customerId", "CUS001").param("rentalDate", "2026-10-08").param("expectedReturnDate", "2026-10-13"))
                .andExpect(status().isOk()).andExpect(view().name("rentals/custom-new"));
        mockMvc.perform(customPost().param("customerId", "CUS001").param("rentalDate", "2026-10-08").param("expectedReturnDate", "2026-10-13")
                        .param("items[0].equipmentId", " ").param("items[0].quantity", "1"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("customRentalDraftForm", "items[0].equipmentId"));
        verifyNoInteractions(rentalService);
    }

    @Test void rejectsInvalidCustomQuantityAndDatesWithoutDelegating() throws Exception {
        for (String quantity : List.of("0", "-1", "not-a-number")) {
            mockMvc.perform(customPost().param("customerId", "CUS001").param("rentalDate", "2026-10-08").param("expectedReturnDate", "2026-10-13")
                            .param("items[0].equipmentId", "EQ001").param("items[0].quantity", quantity))
                    .andExpect(status().isOk()).andExpect(view().name("rentals/custom-new"));
        }
        mockMvc.perform(customPost().param("customerId", "CUS001").param("rentalDate", "2026-10-13").param("expectedReturnDate", "2026-10-08")
                        .param("items[0].equipmentId", "EQ001").param("items[0].quantity", "1"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("customRentalDraftForm", "expectedReturnDate"));
        verifyNoInteractions(rentalService);
    }

    @Test void rejectsForgedCustomPricingStateAndPaymentFields() throws Exception {
        mockMvc.perform(customPost().param("customerId", "CUS001").param("rentalDate", "2026-10-08").param("expectedReturnDate", "2026-10-13")
                        .param("items[0].equipmentId", "EQ001").param("items[0].quantity", "1")
                        .param("items[0].unitPrice", "0.01").param("subtotal", "0.01").param("total", "0.01")
                        .param("paymentStatus", "PAID").param("currentState", "RENTED"))
                .andExpect(status().isOk()).andExpect(view().name("rentals/custom-new"))
                .andExpect(result -> assertUnsupportedFields(result, "customRentalDraftForm"));
        verifyNoInteractions(rentalService);
    }

    @Test void rejectsSparseCustomItemsWithoutDelegating() throws Exception {
        mockMvc.perform(customPost().param("customerId", "CUS001").param("rentalDate", "2026-10-08").param("expectedReturnDate", "2026-10-13")
                        .param("items[1].equipmentId", "EQ001").param("items[1].quantity", "1"))
                .andExpect(status().isBadRequest()).andExpect(view().name("rentals/custom-new"));
        verifyNoInteractions(rentalService);
    }

    @Test void rejectsElevenCustomRowsWithoutDelegating() throws Exception {
        mockMvc.perform(customPostWithRows(11)).andExpect(status().isBadRequest()).andExpect(view().name("error"));
        verifyNoInteractions(rentalService);
    }

    @Test void rejectsOutOfRangeCustomItemIndexWithoutDelegating() throws Exception {
        mockMvc.perform(customPost().param("customerId", "CUS001").param("rentalDate", "2026-10-08").param("expectedReturnDate", "2026-10-13")
                        .param("items[100].equipmentId", "EQ001").param("items[100].quantity", "1"))
                .andExpect(status().isBadRequest()).andExpect(view().name("error"));
        verifyNoInteractions(rentalService);
    }

    @Test void rejectsEquipmentOverrideForOtherwiseValidDraft() throws Exception {
        mockMvc.perform(post("/rentals").contentType(MediaType.APPLICATION_FORM_URLENCODED).param("customerId", "CUS001")
                        .param("rentalDate", "2026-10-08").param("expectedReturnDate", "2026-10-13")
                        .param("packageType", "SOLO").param("equipmentId", "EQ999").session(authenticatedSession()))
                .andExpect(status().isOk()).andExpect(view().name("rentals/new"))
                .andExpect(result -> {
                    BindingResult errors = (BindingResult) result.getModelAndView().getModel()
                            .get(BindingResult.MODEL_KEY_PREFIX + "rentalDraftForm");
                    assertTrue(errors.getGlobalErrors().stream().anyMatch(error -> "unsupportedFields".equals(error.getCode())));
                });
        verifyNoInteractions(campingPackageDraftService);
    }

    @Test void confirmsPendingRentalByDelegatingToService() throws Exception {
        mockMvc.perform(post("/rentals/RENT001/confirm").session(authenticatedSession())).andExpect(redirectedUrl("/rentals/RENT001"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("success", "Đã xác nhận đơn thuê."));
        verify(rentalService).confirmRental("RENT001");
    }

    @Test void cancelsPendingRentalByDelegatingToService() throws Exception {
        mockMvc.perform(post("/rentals/RENT001/cancel").session(authenticatedSession())).andExpect(redirectedUrl("/rentals/RENT001"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("success", "Đã hủy đơn thuê."));
        verify(rentalService).cancelRental("RENT001");
    }

    @Test void rentsConfirmedRentalByDelegatingToService() throws Exception {
        mockMvc.perform(post("/rentals/RENT001/rent").session(authenticatedSession())).andExpect(redirectedUrl("/rentals/RENT001"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("success", "Đơn thuê đã chuyển sang đang cho thuê."));
        verify(rentalService).rentRental("RENT001");
    }

    @Test void cancelsConfirmedRentalByDelegatingToService() throws Exception {
        mockMvc.perform(post("/rentals/RENT002/cancel").session(authenticatedSession())).andExpect(redirectedUrl("/rentals/RENT002"));
        verify(rentalService).cancelRental("RENT002");
    }

    @Test void returnsRentedRentalByDelegatingToService() throws Exception {
        mockMvc.perform(post("/rentals/RENT001/return").session(authenticatedSession())).andExpect(redirectedUrl("/rentals/RENT001"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("success", "Đã ghi nhận trả thiết bị."));
        verify(rentalService).returnRental("RENT001", LocalDate.now());
    }

    @Test void rejectsInvalidOrRepeatedTransitionSafely() throws Exception {
        when(rentalService.rentRental("RENT001")).thenThrow(new IllegalStateException("invalid transition"));
        mockMvc.perform(post("/rentals/RENT001/rent").session(authenticatedSession())).andExpect(redirectedUrl("/rentals/RENT001"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("error", "Không thể thực hiện thao tác vòng đời đơn thuê."));
        verify(rentalService).rentRental("RENT001");
    }

    @Test void handlesUnknownRentalAndInsufficientStockSafely() throws Exception {
        when(rentalService.confirmRental("MISSING")).thenThrow(new IllegalArgumentException("not found"));
        when(rentalService.confirmRental("RENT001")).thenThrow(new IllegalStateException("insufficient stock"));
        mockMvc.perform(post("/rentals/MISSING/confirm").session(authenticatedSession())).andExpect(redirectedUrl("/rentals/MISSING"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("error", "Không thể thực hiện thao tác vòng đời đơn thuê."));
        mockMvc.perform(post("/rentals/RENT001/confirm").session(authenticatedSession())).andExpect(redirectedUrl("/rentals/RENT001"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("error", "Không thể thực hiện thao tác vòng đời đơn thuê."));
        verify(rentalService).confirmRental("MISSING");
        verify(rentalService).confirmRental("RENT001");
    }

    @Test void detailTemplateLinksToCheckoutForTheCurrentRental() throws Exception {
        String template = new String(getClass().getResourceAsStream("/templates/rentals/detail.html").readAllBytes());
        assertTrue(template.contains("@{/checkout/{id}(id=${order.id})}"));
    }

    @Test void rentalTemplatesUseVietnameseLabelsAndServerDefinedPackageContents() throws Exception {
        String list = new String(getClass().getResourceAsStream("/templates/rentals/list.html").readAllBytes());
        String detail = new String(getClass().getResourceAsStream("/templates/rentals/detail.html").readAllBytes());
        String draft = new String(getClass().getResourceAsStream("/templates/rentals/new.html").readAllBytes());
        assertTrue(list.contains("Đơn thuê") && list.contains("fragments/status-badges"));
        assertTrue(detail.contains("Đơn giá tại thời điểm tạo đơn") && detail.contains("fragments/status-badges"));
        assertTrue(draft.contains("packageItems.get(type)") && draft.contains("@presentationLabels.packageType(type)"));
        assertTrue(draft.contains("item.name + ' × ' + item.quantity") && draft.contains("item.equipmentId"));
        assertTrue(draft.contains("type=\"radio\"") && draft.contains("th:field=\"*{packageType}\""));
        assertTrue(draft.contains("/css/package-rental.css"));
    }

    private static RentalOrder order(String id) {
        RentalDetail detail = new RentalDetail(); detail.setEquipmentId("EQ001"); detail.setQuantity(2); detail.setUnitPrice(new BigDecimal("100.00"));
        RentalOrder order = new RentalOrder(); order.setId(id); order.setCustomerId("CUS001"); order.setRentalDate(LocalDate.of(2026, 10, 5));
        order.setExpectedReturnDate(LocalDate.of(2026, 10, 7)); order.setPaymentStatus(PaymentStatus.UNPAID); order.setDetails(List.of(detail)); return order;
    }

    private static MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebLoginController.AUTHENTICATED_USER_ATTRIBUTE, "admin");
        return session;
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder customPost() {
        return post("/rentals/custom").contentType(MediaType.APPLICATION_FORM_URLENCODED).session(authenticatedSession());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder customPostWithRows(int count) {
        var request = customPost().param("customerId", "CUS001").param("rentalDate", "2026-10-08").param("expectedReturnDate", "2026-10-13");
        for (int index = 0; index < count; index++) request.param("items[" + index + "].equipmentId", "EQ" + index).param("items[" + index + "].quantity", String.valueOf(index + 1));
        return request;
    }

    private static void assertUnsupportedFields(org.springframework.test.web.servlet.MvcResult result, String formName) {
        BindingResult errors = (BindingResult) result.getModelAndView().getModel().get(BindingResult.MODEL_KEY_PREFIX + formName);
        assertTrue(errors.getGlobalErrors().stream().anyMatch(error -> "unsupportedFields".equals(error.getCode())));
    }

    private static Equipment equipment(String id) {
        return new Equipment(id, "Tent", "CAT001", new BigDecimal("100.00"), 5, 5, EquipmentStatus.AVAILABLE);
    }

    @TestConfiguration
    static class AuthenticatedRoutesConfiguration implements WebMvcConfigurer {
        @Override public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new AuthenticationInterceptor()).addPathPatterns("/**")
                    .excludePathPatterns("/", "/login", "/error", "/favicon.ico", "/css/**", "/js/**", "/images/**", "/webjars/**");
        }
    }
}
