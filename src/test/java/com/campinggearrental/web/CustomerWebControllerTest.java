package com.campinggearrental.web;

import com.campinggearrental.model.Customer;
import com.campinggearrental.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CustomerWebController.class)
@Import(WebConfiguration.class)
class CustomerWebControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void listShowsCustomersForAuthenticatedEmployee() throws Exception {
        Customer customer = customer();
        when(customerService.search(null)).thenReturn(List.of(customer));

        mockMvc.perform(get("/customers").session(authenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Danh sách và thông tin liên hệ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("customer-search")))
                .andExpect(model().attribute("customers", List.of(customer)));
    }

    @Test
    void searchPassesKeywordToCustomerService() throws Exception {
        when(customerService.search("An")).thenReturn(List.of(customer()));

        mockMvc.perform(get("/customers").param("q", "An").session(authenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/list"))
                .andExpect(model().attribute("keyword", "An"));

        verify(customerService).search("An");
    }

    @Test
    void unauthenticatedCustomerRouteRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void detailLoadsCustomerById() throws Exception {
        when(customerService.findById("CUS001")).thenReturn(Optional.of(customer()));

        mockMvc.perform(get("/customers/CUS001").session(authenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("customer-details-card")))
                .andExpect(model().attribute("customer", customer()));
    }

    @Test
    void newCustomerFormRendersVietnameseFields() throws Exception {
        mockMvc.perform(get("/customers/new").session(authenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Họ tên")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("customer-form-card")));
    }

    @Test
    void createPersistsCustomerAndRedirectsToDetail() throws Exception {
        when(customerService.add("Nguyen An", "0900000001", "an@example.com", "Da Lat"))
                .thenReturn(customer());

        mockMvc.perform(post("/customers")
                        .param("fullName", "Nguyen An")
                        .param("phone", "0900000001")
                        .param("email", "an@example.com")
                        .param("address", "Da Lat")
                        .session(authenticatedSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/CUS001"))
                .andExpect(flash().attribute("success", "Đã thêm khách hàng."));
    }

    @Test
    void createValidationFailureReturnsFormWithMessage() throws Exception {
        when(customerService.add(null, null, null, null))
                .thenThrow(new IllegalArgumentException("Tên và số điện thoại là bắt buộc."));

        mockMvc.perform(post("/customers").session(authenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/form"))
                .andExpect(model().attribute("error", "Tên và số điện thoại là bắt buộc."));
    }

    @Test
    void editLoadsExistingCustomerAndUpdatePersistsChanges() throws Exception {
        Customer original = customer();
        when(customerService.findById("CUS001")).thenReturn(Optional.of(original));

        mockMvc.perform(get("/customers/CUS001/edit").session(authenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/form"))
                .andExpect(model().attribute("customer", original));

        mockMvc.perform(post("/customers/CUS001")
                        .param("fullName", "Nguyen An Updated")
                        .param("phone", "0900000001")
                        .param("email", "an@example.com")
                        .param("address", "Da Lat")
                        .session(authenticatedSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/CUS001"));

        verify(customerService).update(new Customer("CUS001", "Nguyen An Updated", "0900000001",
                "an@example.com", "Da Lat"));
    }

    @Test
    void databaseFailureShowsGenericMessage() throws Exception {
        when(customerService.search(null)).thenThrow(new SQLException("database-secret"));

        mockMvc.perform(get("/customers").session(authenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/list"))
                .andExpect(model().attribute("error", "Không thể tải danh sách khách hàng lúc này."));
    }

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebLoginController.AUTHENTICATED_USER_ATTRIBUTE, "admin");
        return session;
    }

    private Customer customer() {
        return new Customer("CUS001", "Nguyen An", "0900000001", "an@example.com", "Da Lat");
    }
}
