package com.campinggearrental.web;

import com.campinggearrental.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(WebLoginController.class)
@Import(WebConfiguration.class)
class WebLoginControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void getLoginDisplaysTheLoginFormWithoutCreatingASession() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(containsString("login-mark")))
                .andExpect(content().string(containsString("Tên đăng nhập")))
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(request().sessionAttributeDoesNotExist(WebLoginController.AUTHENTICATED_USER_ATTRIBUTE));
    }

    @Test
    void validLoginCreatesAuthenticatedSessionAndRedirectsToDashboard() throws Exception {
        when(authService.login("admin", "admin123")).thenReturn(true);

        mockMvc.perform(post("/login").param("username", "admin").param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(request().sessionAttribute(WebLoginController.AUTHENTICATED_USER_ATTRIBUTE, "admin"));
    }

    @Test
    void invalidLoginShowsErrorAndDoesNotCreateSession() throws Exception {
        when(authService.login("admin", "wrong")).thenReturn(false);

        mockMvc.perform(post("/login").param("username", "admin").param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(request().sessionAttributeDoesNotExist(WebLoginController.AUTHENTICATED_USER_ATTRIBUTE));
    }

    @Test
    void unauthenticatedDashboardRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void authenticatedDashboardShowsAllModuleNavigation() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebLoginController.AUTHENTICATED_USER_ATTRIBUTE, "admin");

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(containsString("Tổng quan")))
                .andExpect(content().string(containsString("Khách hàng")))
                .andExpect(content().string(containsString("Thiết bị")))
                .andExpect(content().string(containsString("Đơn thuê")))
                .andExpect(content().string(containsString("Thanh toán")))
                .andExpect(content().string(containsString("Đăng xuất")))
                .andExpect(content().string(containsString("Truy cập nhanh")))
                .andExpect(content().string(containsString("dashboard-link-card")))
                .andExpect(content().string(containsString("action=\"/logout\"")))
                .andExpect(content().string(containsString("bootstrap.bundle.min.js")))
                .andExpect(content().string(containsString("href=\"/customers\"")))
                .andExpect(content().string(containsString("href=\"/equipment\"")))
                .andExpect(content().string(containsString("href=\"/rentals\"")))
                .andExpect(content().string(containsString("href=\"/checkout\"")));
    }

    @Test
    void logoutInvalidatesSessionAndRedirectsToLogin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebLoginController.AUTHENTICATED_USER_ATTRIBUTE, "admin");

        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertTrue(session.isInvalid());
    }

    @Test
    void authenticationDatabaseErrorDoesNotExposeExceptionDetails() throws Exception {
        when(authService.login("admin", "admin123"))
                .thenThrow(new SQLException("password=database-secret"));

        var result = mockMvc.perform(post("/login").param("username", "admin").param("password", "admin123"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andReturn();

        assertNull(result.getRequest().getSession(false));
        String renderedPage = result.getResponse().getContentAsString();
        assertTrue(renderedPage.contains("Không thể đăng nhập lúc này."));
        assertTrue(!renderedPage.contains("database-secret"));
        assertTrue(!renderedPage.contains("SQLException"));
    }
}
