package com.campinggearrental.web;

import com.campinggearrental.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationInterceptorTest {
    private final AuthenticationInterceptor interceptor = new AuthenticationInterceptor();

    @Test void customerCannotAccessAdminRoutesOrLifecycleActions() throws Exception {
        assertForbidden("GET", "/customers");
        assertForbidden("GET", "/equipment/new");
        assertForbidden("POST", "/rentals/R1/confirm");
        assertForbidden("POST", "/checkout/R1/pay");
    }

    @Test void customerCanBrowseAndCreateRentals() throws Exception {
        assertAllowed("GET", "/equipment");
        assertAllowed("GET", "/rentals/R1");
        assertAllowed("POST", "/rentals");
        assertAllowed("POST", "/rentals/R1/cancel");
        assertAllowed("POST", "/checkout/R1/refresh");
        assertAllowed("POST", "/checkout/R1/payment-method");
        assertAllowed("POST", "/checkout/R1/report-transfer");
    }

    @Test void rejectsMissingOrWrongCsrfTokenForAuthenticatedMutation() throws Exception {
        CsrfProtectionInterceptor csrf = new CsrfProtectionInterceptor();
        MockHttpServletRequest missing = customerRequest("POST", "/rentals");
        missing.getSession().setAttribute(WebLoginController.CSRF_TOKEN_ATTRIBUTE, "expected-token");
        MockHttpServletResponse missingResponse = new MockHttpServletResponse();
        assertFalse(csrf.preHandle(missing, missingResponse, new Object()));
        assertEquals(403, missingResponse.getStatus());

        MockHttpServletRequest wrong = customerRequest("POST", "/rentals");
        wrong.getSession().setAttribute(WebLoginController.CSRF_TOKEN_ATTRIBUTE, "expected-token");
        wrong.addParameter("_csrf", "wrong-token");
        assertFalse(csrf.preHandle(wrong, new MockHttpServletResponse(), new Object()));

        MockHttpServletRequest valid = customerRequest("POST", "/rentals");
        valid.getSession().setAttribute(WebLoginController.CSRF_TOKEN_ATTRIBUTE, "expected-token");
        valid.addParameter("_csrf", "expected-token");
        assertTrue(csrf.preHandle(valid, new MockHttpServletResponse(), new Object()));

        MockHttpServletRequest checkout = customerRequest("POST", "/checkout/R1/payment-method");
        checkout.getSession().setAttribute(WebLoginController.CSRF_TOKEN_ATTRIBUTE, "expected-token");
        assertFalse(csrf.preHandle(checkout, new MockHttpServletResponse(), new Object()));
    }

    private void assertForbidden(String method, String path) throws Exception {
        MockHttpServletRequest request = customerRequest(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(403, response.getStatus());
    }

    private void assertAllowed(String method, String path) throws Exception {
        assertTrue(interceptor.preHandle(customerRequest(method, path), new MockHttpServletResponse(), new Object()));
    }

    private MockHttpServletRequest customerRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.getSession().setAttribute(WebLoginController.AUTHENTICATED_USER_ATTRIBUTE, "customer");
        request.getSession().setAttribute(WebLoginController.AUTHENTICATED_ROLE_ATTRIBUTE, UserRole.CUSTOMER);
        request.getSession().setAttribute(WebLoginController.AUTHENTICATED_CUSTOMER_ATTRIBUTE, "C1");
        return request;
    }
}
