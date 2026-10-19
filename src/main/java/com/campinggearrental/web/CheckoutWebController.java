package com.campinggearrental.web;

import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.Customer;
import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.PaymentMethod;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.model.RentalOrderStatus;
import com.campinggearrental.service.EquipmentService;
import com.campinggearrental.service.CustomerService;
import com.campinggearrental.service.PersistedCheckoutService;
import com.campinggearrental.service.PricingService;
import com.campinggearrental.service.RentalOrderService;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import com.campinggearrental.model.UserRole;

@Controller
public class CheckoutWebController {
    private final PersistedCheckoutService checkout;
    private final PricingService pricing;
    private final RentalOrderService rentalOrders;
    private final EquipmentService equipment;
    private final CustomerService customers;
    @Value("${camping.payment.bank-name:}") private String bankName;
    @Value("${camping.payment.account-number:}") private String accountNumber;
    @Value("${camping.payment.account-name:}") private String accountName;

    public CheckoutWebController(PersistedCheckoutService checkout, PricingService pricing,
                                 RentalOrderService rentalOrders, EquipmentService equipment, CustomerService customers) {
        this.checkout = checkout;
        this.pricing = pricing;
        this.rentalOrders = rentalOrders;
        this.equipment = equipment;
        this.customers = customers;
    }

    @GetMapping("/checkout")
    String lookup(Model model, HttpSession session) {
        try {
            List<RentalOrder> rentals = rentalOrders.findAll().stream()
                    .filter(order -> isAdmin(session) || order.getCustomerId().equals(customerId(session)))
                    .sorted(Comparator.comparing(order -> order.getPaymentStatus() != PaymentStatus.UNPAID))
                    .toList();
            model.addAttribute("rentals", rentals);
            model.addAttribute("rentalStates", rentalStates(rentals));
            model.addAttribute("customerNames", customerNames());
        } catch (SQLException exception) {
            model.addAttribute("rentals", List.of());
            model.addAttribute("rentalStates", Map.of());
            model.addAttribute("customerNames", Map.of());
            model.addAttribute("error", "Không thể tải danh sách đơn thuê.");
        }
        return "checkout/lookup";
    }

    @GetMapping("/checkout/{rentalId}")
    String detail(@PathVariable("rentalId") String rentalId, Model model, RedirectAttributes flash,
                  HttpSession session, HttpServletResponse response) {
        try {
            RentalOrder order = checkout.load(rentalId);
            if (!isAdmin(session) && !order.getCustomerId().equals(customerId(session))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return "error";
            }
            model.addAttribute("order", order);
            model.addAttribute("rentalState", RentalOrderStatus.fromState(order.getCurrentState()).name());
            model.addAttribute("rentalDays", pricing.calculateRentalDays(order.getRentalDate(), order.getExpectedReturnDate()));
            model.addAttribute("equipmentNames", equipmentNames());
            model.addAttribute("paymentMethods", List.of(PaymentMethod.CASH, PaymentMethod.BANK_TRANSFER));
            model.addAttribute("bankConfigured", bankTransferReady(order));
            model.addAttribute("bankName", bankName);
            model.addAttribute("accountNumber", accountNumber);
            model.addAttribute("accountName", accountName);
            return "checkout/detail";
        } catch (IllegalArgumentException | SQLException exception) {
            flash.addFlashAttribute("error", "Không tìm thấy hoặc không thể tải đơn thuê.");
            return "redirect:/checkout";
        }
    }

    @PostMapping("/checkout/load")
    String load(@RequestParam("rentalId") String rentalId, RedirectAttributes flash) {
        if (rentalId == null || rentalId.isBlank()) {
            flash.addFlashAttribute("error", "Cần nhập mã đơn thuê.");
            return "redirect:/checkout";
        }
        flash.addFlashAttribute("success", "Đã tải đơn thuê.");
        return "redirect:/checkout/" + rentalId.trim();
    }

    @PostMapping("/checkout/{rentalId}/refresh")
    String refresh(@PathVariable("rentalId") String rentalId, RedirectAttributes flash,
                   HttpSession session, HttpServletResponse response) {
        try {
            RentalOrder order = checkout.load(rentalId);
            if (!isAdmin(session) && !order.getCustomerId().equals(customerId(session))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return "error";
            }
            checkout.refreshPricing(rentalId);
            flash.addFlashAttribute("success", "Đã cập nhật giá thuê.");
        } catch (IllegalArgumentException | SQLException exception) {
            flash.addFlashAttribute("error", "Không thể cập nhật giá thuê.");
        }
        return "redirect:/checkout/" + rentalId;
    }

    @PostMapping("/checkout/{rentalId}/pay")
    String pay(@PathVariable("rentalId") String rentalId, RedirectAttributes flash,
               HttpSession session, HttpServletResponse response) {
        if (!isAdmin(session)) return forbidden(response);
        try {
            checkout.confirmPayment(rentalId);
            flash.addFlashAttribute("success", "Đã xác nhận thanh toán.");
        } catch (IllegalArgumentException | IllegalStateException | SQLException exception) {
            flash.addFlashAttribute("error", "Không thể xác nhận thanh toán.");
        }
        return "redirect:/checkout/" + rentalId;
    }

    @PostMapping("/checkout/{rentalId}/payment-method")
    String selectPaymentMethod(@PathVariable("rentalId") String rentalId, @RequestParam("paymentMethod") String value,
                               RedirectAttributes flash, HttpSession session, HttpServletResponse response) {
        try {
            RentalOrder order = checkout.load(rentalId);
            if (isAdmin(session) || !order.getCustomerId().equals(customerId(session))) return forbidden(response);
            PaymentMethod method = PaymentMethod.valueOf(value);
            if (method != PaymentMethod.CASH && method != PaymentMethod.BANK_TRANSFER) {
                throw new IllegalArgumentException("payment method is unavailable");
            }
            checkout.selectPaymentMethod(rentalId, method);
            flash.addFlashAttribute("success", "Đã lưu phương thức thanh toán.");
        } catch (IllegalArgumentException | IllegalStateException | SQLException exception) {
            flash.addFlashAttribute("error", "Không thể lưu phương thức thanh toán.");
        }
        return "redirect:/checkout/" + rentalId;
    }

    @PostMapping("/checkout/{rentalId}/report-transfer")
    String reportTransfer(@PathVariable("rentalId") String rentalId, RedirectAttributes flash,
                          HttpSession session, HttpServletResponse response) {
        try {
            RentalOrder order = checkout.load(rentalId);
            if (isAdmin(session) || !order.getCustomerId().equals(customerId(session))) return forbidden(response);
            if (!bankTransferReady(order)) {
                flash.addFlashAttribute("error", "Không thể hiển thị mã chuyển khoản do thiếu cấu hình ngân hàng, ảnh QR hoặc số tiền không hợp lệ.");
                return "redirect:/checkout/" + rentalId;
            }
            checkout.reportTransfer(rentalId);
            flash.addFlashAttribute("success", "Đã gửi thông báo chuyển khoản; quản trị viên sẽ kiểm tra.");
        } catch (IllegalArgumentException | IllegalStateException | SQLException exception) {
            flash.addFlashAttribute("error", "Không thể gửi thông báo chuyển khoản.");
        }
        return "redirect:/checkout/" + rentalId;
    }

    @PostMapping("/checkout/{rentalId}/payment-review")
    String requestPaymentReview(@PathVariable("rentalId") String rentalId, RedirectAttributes flash,
                                HttpSession session, HttpServletResponse response) {
        if (!isAdmin(session)) return forbidden(response);
        return adminPaymentAction(rentalId, flash, () -> checkout.requestReview(rentalId),
                "Đã yêu cầu khách hàng kiểm tra lại giao dịch.");
    }

    @PostMapping("/checkout/{rentalId}/payment-reject")
    String rejectPayment(@PathVariable("rentalId") String rentalId, RedirectAttributes flash,
                         HttpSession session, HttpServletResponse response) {
        if (!isAdmin(session)) return forbidden(response);
        return adminPaymentAction(rentalId, flash, () -> checkout.rejectTransfer(rentalId),
                "Đã từ chối thông báo chuyển khoản.");
    }

    private String adminPaymentAction(String rentalId, RedirectAttributes flash, SqlAction action, String success) {
        try {
            action.run();
            flash.addFlashAttribute("success", success);
        } catch (IllegalArgumentException | IllegalStateException | SQLException exception) {
            flash.addFlashAttribute("error", "Không thể cập nhật trạng thái thanh toán.");
        }
        return "redirect:/checkout/" + rentalId;
    }

    private Map<String, String> equipmentNames() throws SQLException {
        Map<String, String> names = new LinkedHashMap<>();
        for (Equipment item : equipment.list()) names.put(item.getEquipmentId(), item.getName());
        return names;
    }

    private Map<String, String> rentalStates(List<RentalOrder> rentals) {
        Map<String, String> states = new LinkedHashMap<>();
        for (RentalOrder order : rentals) {
            states.put(order.getId(), RentalOrderStatus.fromState(order.getCurrentState()).name());
        }
        return states;
    }

    private Map<String, String> customerNames() {
        try {
            Map<String, String> names = new LinkedHashMap<>();
            for (Customer customer : customers.findAll()) names.put(customer.id(), customer.fullName());
            return names;
        } catch (SQLException ignored) {
            return Map.of();
        }
    }

    private boolean isAdmin(HttpSession session) {
        return session.getAttribute(WebLoginController.AUTHENTICATED_ROLE_ATTRIBUTE) == UserRole.ADMIN;
    }

    private String customerId(HttpSession session) {
        Object value = session.getAttribute(WebLoginController.AUTHENTICATED_CUSTOMER_ATTRIBUTE);
        return value == null ? "" : value.toString();
    }

    private String forbidden(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return "error";
    }

    private boolean configured(String value) { return value != null && !value.isBlank(); }

    private boolean bankTransferReady(RentalOrder order) {
        if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER && order.getPaymentMethod() != PaymentMethod.VIETQR) return false;
        return configured(bankName) && configured(accountNumber) && configured(accountName)
                && order.getTotal() != null && order.getTotal().signum() > 0 && configured(order.getId())
                && new ClassPathResource("static/images/payment/bank-qr.png").exists();
    }

    @FunctionalInterface private interface SqlAction { void run() throws SQLException; }
}
