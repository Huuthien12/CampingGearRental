package com.campinggearrental.web;

import com.campinggearrental.model.Customer;
import com.campinggearrental.service.CustomerService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Controller
public class CustomerWebController {
    private static final String FORM_VIEW = "customers/form";

    private final CustomerService customerService;

    public CustomerWebController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/customers")
    public String list(@RequestParam(name = "q", required = false) String keyword, Model model) {
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        try {
            model.addAttribute("customers", customerService.search(keyword));
        } catch (SQLException exception) {
            model.addAttribute("customers", List.of());
            model.addAttribute("error", "Không thể tải danh sách khách hàng lúc này.");
        }
        return "customers/list";
    }

    @GetMapping("/customers/new")
    public String newCustomer(Model model) {
        prepareForm(model, emptyCustomer(), "/customers", "Thêm khách hàng");
        return FORM_VIEW;
    }

    @PostMapping("/customers")
    public String create(@RequestParam(name = "fullName", required = false) String fullName,
                         @RequestParam(name = "phone", required = false) String phone,
                         @RequestParam(name = "email", required = false) String email,
                         @RequestParam(name = "address", required = false) String address,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Customer formCustomer = new Customer("", value(fullName), value(phone), value(email), value(address));
        try {
            Customer created = customerService.add(fullName, phone, email, address);
            redirectAttributes.addFlashAttribute("success", "Đã thêm khách hàng.");
            return "redirect:/customers/" + created.id();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
        } catch (SQLException exception) {
            model.addAttribute("error", "Không thể lưu khách hàng lúc này.");
        }
        prepareForm(model, formCustomer, "/customers", "Thêm khách hàng");
        return FORM_VIEW;
    }

    @GetMapping("/customers/{id}")
    public String detail(@PathVariable(name = "id") String id, Model model,
                         HttpServletResponse response) throws IOException {
        try {
            Optional<Customer> customer = customerService.findById(id);
            if (customer.isEmpty()) {
                return notFound(model, response);
            }
            model.addAttribute("customer", customer.get());
            return "customers/detail";
        } catch (SQLException exception) {
            return databaseError(model, response);
        }
    }

    @GetMapping("/customers/{id}/edit")
    public String edit(@PathVariable(name = "id") String id, Model model,
                       HttpServletResponse response) throws IOException {
        try {
            Optional<Customer> customer = customerService.findById(id);
            if (customer.isEmpty()) {
                return notFound(model, response);
            }
            prepareForm(model, customer.get(), "/customers/" + customer.get().id(), "Sửa khách hàng");
            return FORM_VIEW;
        } catch (SQLException exception) {
            return databaseError(model, response);
        }
    }

    @PostMapping("/customers/{id}")
    public String update(@PathVariable(name = "id") String id,
                         @RequestParam(name = "fullName", required = false) String fullName,
                         @RequestParam(name = "phone", required = false) String phone,
                         @RequestParam(name = "email", required = false) String email,
                         @RequestParam(name = "address", required = false) String address,
                         Model model,
                         RedirectAttributes redirectAttributes,
                         HttpServletResponse response) throws IOException {
        Customer formCustomer = new Customer(id, value(fullName), value(phone), value(email), value(address));
        try {
            if (customerService.findById(id).isEmpty()) {
                return notFound(model, response);
            }
            customerService.update(formCustomer);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật khách hàng.");
            return "redirect:/customers/" + id;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
        } catch (SQLException exception) {
            model.addAttribute("error", "Không thể lưu khách hàng lúc này.");
        }
        prepareForm(model, formCustomer, "/customers/" + id, "Sửa khách hàng");
        return FORM_VIEW;
    }

    private void prepareForm(Model model, Customer customer, String action, String title) {
        model.addAttribute("customer", customer);
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
    }

    private String notFound(Model model, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("message", "Không tìm thấy khách hàng.");
        return "error";
    }

    private String databaseError(Model model, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        model.addAttribute("message", "Không thể tải thông tin khách hàng lúc này.");
        return "error";
    }

    private Customer emptyCustomer() {
        return new Customer("", "", "", "", "");
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}