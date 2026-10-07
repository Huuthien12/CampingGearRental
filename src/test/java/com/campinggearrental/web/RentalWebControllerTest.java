package com.campinggearrental.web;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
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
import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalDetail;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.service.CampingPackageDraftService;
import com.campinggearrental.service.RentalOrderService;
import com.campinggearrental.service.RentalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RentalWebController.class)
class RentalWebControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private CampingPackageDraftService campingPackageDraftService;
    @MockBean private RentalOrderService rentalOrderService;
    @MockBean private RentalService rentalService;

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
        mockMvc.perform(get("/rentals/new").session(authenticatedSession())).andExpect(status().isOk()).andExpect(view().name("rentals/new"))
                .andExpect(model().attributeExists("rentalDraftForm", "packageTypes"));
    }

    @Test void createsDraftFromServerDefinedPackage() throws Exception {
        RentalOrder order = order("RENT001");
        when(campingPackageDraftService.createDraftFromPackage(eq("CUS001"), eq(LocalDate.of(2026, 10, 5)),
                eq(LocalDate.of(2026, 10, 7)), eq(CampingPackageType.COUPLE))).thenReturn(order);
        mockMvc.perform(post("/rentals").param("customerId", " CUS001 ").param("rentalDate", "2026-10-05")
                        .param("expectedReturnDate", "2026-10-07").param("packageType", "COUPLE").session(authenticatedSession()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/rentals/RENT001"));
        verify(campingPackageDraftService).createDraftFromPackage("CUS001", LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 7), CampingPackageType.COUPLE);
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

    @Test void confirmsPendingRentalByDelegatingToService() throws Exception {
        mockMvc.perform(post("/rentals/RENT001/confirm")).andExpect(redirectedUrl("/rentals/RENT001"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("success", "Rental confirmed."));
        verify(rentalService).confirmRental("RENT001");
    }

    @Test void cancelsPendingRentalByDelegatingToService() throws Exception {
        mockMvc.perform(post("/rentals/RENT001/cancel")).andExpect(redirectedUrl("/rentals/RENT001"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("success", "Rental cancelled."));
        verify(rentalService).cancelRental("RENT001");
    }

    @Test void rentsConfirmedRentalByDelegatingToService() throws Exception {
        mockMvc.perform(post("/rentals/RENT001/rent")).andExpect(redirectedUrl("/rentals/RENT001"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("success", "Rental marked as rented."));
        verify(rentalService).rentRental("RENT001");
    }

    @Test void cancelsConfirmedRentalByDelegatingToService() throws Exception {
        mockMvc.perform(post("/rentals/RENT002/cancel")).andExpect(redirectedUrl("/rentals/RENT002"));
        verify(rentalService).cancelRental("RENT002");
    }

    @Test void returnsRentedRentalByDelegatingToService() throws Exception {
        mockMvc.perform(post("/rentals/RENT001/return")).andExpect(redirectedUrl("/rentals/RENT001"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("success", "Rental returned."));
        verify(rentalService).returnRental("RENT001", LocalDate.now());
    }

    @Test void rejectsInvalidOrRepeatedTransitionSafely() throws Exception {
        when(rentalService.rentRental("RENT001")).thenThrow(new IllegalStateException("invalid transition"));
        mockMvc.perform(post("/rentals/RENT001/rent")).andExpect(redirectedUrl("/rentals/RENT001"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("error", "Rental lifecycle action could not be completed."));
        verify(rentalService).rentRental("RENT001");
    }

    @Test void handlesUnknownRentalAndInsufficientStockSafely() throws Exception {
        when(rentalService.confirmRental("MISSING")).thenThrow(new IllegalArgumentException("not found"));
        when(rentalService.confirmRental("RENT001")).thenThrow(new IllegalStateException("insufficient stock"));
        mockMvc.perform(post("/rentals/MISSING/confirm")).andExpect(redirectedUrl("/rentals/MISSING"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("error", "Rental lifecycle action could not be completed."));
        mockMvc.perform(post("/rentals/RENT001/confirm")).andExpect(redirectedUrl("/rentals/RENT001"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("error", "Rental lifecycle action could not be completed."));
        verify(rentalService).confirmRental("MISSING");
        verify(rentalService).confirmRental("RENT001");
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
}
