package com.campinggearrental.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import com.campinggearrental.model.UserRole;

public class AuthenticationInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Object authenticatedUser = request.getSession(false) == null
                ? null
                : request.getSession(false).getAttribute(WebLoginController.AUTHENTICATED_USER_ATTRIBUTE);
        if (authenticatedUser instanceof String username && !username.isBlank()) {
            UserRole role = (UserRole) request.getSession(false).getAttribute(WebLoginController.AUTHENTICATED_ROLE_ATTRIBUTE);
            if (role == UserRole.ADMIN || customerMayAccess(request)) return true;
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }

    private boolean customerMayAccess(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("GET".equals(method) && (path.equals("/dashboard") || path.equals("/equipment")
                || path.startsWith("/rentals") || path.equals("/checkout") || path.startsWith("/checkout/"))) return true;
        if ("POST".equals(method) && (path.equals("/logout") || path.equals("/rentals")
                || path.equals("/rentals/custom") || path.matches("/rentals/[^/]+/cancel")
                || path.matches("/checkout/[^/]+/refresh")
                || path.matches("/checkout/[^/]+/payment-method")
                || path.matches("/checkout/[^/]+/report-transfer"))) return true;
        return false;
    }
}
