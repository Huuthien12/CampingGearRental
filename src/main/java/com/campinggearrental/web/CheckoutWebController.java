package com.campinggearrental.web;

import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.Customer;
import com.campinggearrental.model.PaymentStatus;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CheckoutWebController {
    private final PersistedCheckoutService checkout;
    private final PricingService pricing;
    private final RentalOrderService rentalOrders;
    private final EquipmentService equipment;
    private final CustomerService customers;

    public CheckoutWebController(PersistedCheckoutService checkout, PricingService pricing,
                                 RentalOrderService rentalOrders, EquipmentService equipment, CustomerService customers) {
        this.checkout = checkout;
        this.pricing = pricing;
        this.rentalOrders = rentalOrders;
        this.equipment = equipment;
        this.customers = customers;
    }

    @GetMapping("/checkout")
    String lookup(Model model) {
        try {
            List<RentalOrder> rentals = rentalOrders.findAll().stream()
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
    String detail(@PathVariable("rentalId") String rentalId, Model model, RedirectAttributes flash) {
        try {
            RentalOrder order = checkout.load(rentalId);
            model.addAttribute("order", order);
            model.addAttribute("rentalState", RentalOrderStatus.fromState(order.getCurrentState()).name());
            model.addAttribute("rentalDays", pricing.calculateRentalDays(order.getRentalDate(), order.getExpectedReturnDate()));
            model.addAttribute("equipmentNames", equipmentNames());
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
    String refresh(@PathVariable("rentalId") String rentalId, RedirectAttributes flash) {
        try {
            checkout.refreshPricing(rentalId);
            flash.addFlashAttribute("success", "Đã cập nhật giá thuê.");
        } catch (IllegalArgumentException | SQLException exception) {
            flash.addFlashAttribute("error", "Không thể cập nhật giá thuê.");
        }
        return "redirect:/checkout/" + rentalId;
    }

    @PostMapping("/checkout/{rentalId}/pay")
    String pay(@PathVariable("rentalId") String rentalId, RedirectAttributes flash) {
        try {
            checkout.confirmPayment(rentalId);
            flash.addFlashAttribute("success", "Đã xác nhận thanh toán.");
        } catch (IllegalArgumentException | IllegalStateException | SQLException exception) {
            flash.addFlashAttribute("error", "Không thể xác nhận thanh toán.");
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
}
