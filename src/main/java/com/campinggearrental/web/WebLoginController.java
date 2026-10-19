package com.campinggearrental.web;

import com.campinggearrental.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.SQLException;
import com.campinggearrental.model.User;
import com.campinggearrental.model.UserRole;

@Controller
public class WebLoginController {
    public static final String AUTHENTICATED_USER_ATTRIBUTE = "authenticatedUser";
    public static final String AUTHENTICATED_ROLE_ATTRIBUTE = "authenticatedRole";
    public static final String AUTHENTICATED_CUSTOMER_ATTRIBUTE = "authenticatedCustomerId";
    public static final String CSRF_TOKEN_ATTRIBUTE = "csrfToken";
    private static final int SESSION_TIMEOUT_SECONDS = 30 * 60;

    private final AuthService authService;

    public WebLoginController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam(name = "username", required = false) String username,
                        @RequestParam(name = "password", required = false) String password,
                        HttpServletRequest request,
                        Model model) {
        try {
            User user = authService.authenticate(username, password).orElse(null);
            if (user == null) {
                model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng.");
                return "login";
            }
            HttpSession existingSession = request.getSession(false);
            if (existingSession != null) existingSession.invalidate();
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
            session.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, user.username());
            session.setAttribute(AUTHENTICATED_ROLE_ATTRIBUTE, user.role());
            session.setAttribute(AUTHENTICATED_CUSTOMER_ATTRIBUTE, user.customerId());
            CsrfProtectionInterceptor.rotateToken(session);
        } catch (SQLException exception) {
            model.addAttribute("error", "Không thể đăng nhập lúc này. Vui lòng thử lại sau.");
            return "login";
        }

        return "redirect:/dashboard";
    }

    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
    public String register(@RequestParam(name = "username", required = false) String username,
                           @RequestParam(name = "password", required = false) String password,
                           @RequestParam(name = "fullName", required = false) String fullName,
                           @RequestParam(name = "phone", required = false) String phone,
                           @RequestParam(name = "email", required = false) String email,
                           @RequestParam(name = "address", required = false) String address,
                           Model model) {
        try {
            authService.registerCustomer(username, password, fullName, phone, email, address);
            model.addAttribute("success", "Đăng ký thành công. Bạn có thể đăng nhập.");
            return "login";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
        } catch (SQLException exception) {
            model.addAttribute("error", "Không thể đăng ký lúc này. Vui lòng kiểm tra thông tin và thử lại.");
        }
        model.addAttribute("username", username);
        model.addAttribute("fullName", fullName);
        model.addAttribute("phone", phone);
        model.addAttribute("email", email);
        model.addAttribute("address", address);
        return "register";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        model.addAttribute("username", session.getAttribute(AUTHENTICATED_USER_ATTRIBUTE));
        model.addAttribute("isAdmin", session.getAttribute(AUTHENTICATED_ROLE_ATTRIBUTE) == UserRole.ADMIN);
        return "dashboard";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}
