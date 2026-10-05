package com.campinggearrental.web;

import com.campinggearrental.factory.CampingPackageType;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.model.RentalOrderStatus;
import com.campinggearrental.service.CampingPackageDraftService;
import com.campinggearrental.service.RentalOrderService;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/rentals")
public class RentalWebController {
    private final RentalOrderService rentalOrderService;
    private final CampingPackageDraftService campingPackageDraftService;

    public RentalWebController(RentalOrderService rentalOrderService,
            CampingPackageDraftService campingPackageDraftService) {
        this.rentalOrderService = rentalOrderService;
        this.campingPackageDraftService = campingPackageDraftService;
    }

    @GetMapping
    public String list(Model model) throws SQLException {
        List<RentalOrder> rentals = rentalOrderService.findAll();
        Map<String, String> states = new LinkedHashMap<>();
        rentals.forEach(rental -> states.put(rental.getId(), stateName(rental)));
        model.addAttribute("rentals", rentals);
        model.addAttribute("rentalStates", states);
        return "rentals/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") String id, Model model) throws SQLException {
        RentalOrder order = rentalOrderService.findById(id);
        if (order == null) return "error/404";
        model.addAttribute("order", order);
        model.addAttribute("stateName", stateName(order));
        return "rentals/detail";
    }

    @InitBinder("rentalDraftForm")
    public void bindForm(WebDataBinder binder) {
        binder.setAllowedFields("customerId", "rentalDate", "expectedReturnDate", "packageType");
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("rentalDraftForm")) model.addAttribute("rentalDraftForm", new RentalDraftForm());
        model.addAttribute("packageTypes", CampingPackageType.values());
        return "rentals/new";
    }

    @PostMapping
    public String create(@ModelAttribute("rentalDraftForm") RentalDraftForm form, BindingResult errors,
            Model model, RedirectAttributes redirect) {
        rejectUnexpectedFields(errors);
        validate(form, errors);
        if (errors.hasErrors()) return newForm(model);
        try {
            RentalOrder order = campingPackageDraftService.createDraftFromPackage(form.getCustomerId().trim(),
                    form.getRentalDate(), form.getExpectedReturnDate(), form.getPackageType());
            redirect.addFlashAttribute("success", "Rental draft created successfully.");
            return "redirect:/rentals/" + order.getId();
        } catch (IllegalArgumentException exception) {
            errors.reject("invalidRentalDraft", exception.getMessage());
        } catch (SQLException exception) {
            errors.reject("saveFailed", "Unable to create the rental draft. Please try again.");
        }
        return newForm(model);
    }

    private void validate(RentalDraftForm form, BindingResult errors) {
        if (form.getCustomerId() == null || form.getCustomerId().isBlank()) errors.rejectValue("customerId", "required", "Customer ID is required.");
        LocalDate rentalDate = form.getRentalDate();
        LocalDate expectedReturnDate = form.getExpectedReturnDate();
        if (rentalDate == null) errors.rejectValue("rentalDate", "required", "Rental date is required.");
        if (expectedReturnDate == null) errors.rejectValue("expectedReturnDate", "required", "Expected return date is required.");
        else if (rentalDate != null && expectedReturnDate.isBefore(rentalDate)) errors.rejectValue("expectedReturnDate", "invalidDateRange", "Expected return date cannot be before rental date.");
        if (form.getPackageType() == null) errors.rejectValue("packageType", "required", "Choose a camping package.");
    }

    private void rejectUnexpectedFields(BindingResult errors) {
        if (errors.getSuppressedFields().length > 0) errors.reject("unsupportedFields", "Package equipment and quantities are defined by the server.");
    }

    private static String stateName(RentalOrder order) {
        return RentalOrderStatus.fromState(order.getCurrentState()).name();
    }
}
