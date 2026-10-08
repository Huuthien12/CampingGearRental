package com.campinggearrental.web;

import jakarta.servlet.http.HttpServletResponse;
import com.campinggearrental.factory.CampingPackageType;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.model.RentalOrderStatus;
import com.campinggearrental.service.CampingPackageDraftService;
import com.campinggearrental.service.CategoryService;
import com.campinggearrental.service.EquipmentService;
import com.campinggearrental.service.RentalOrderService;
import com.campinggearrental.service.RentalService;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/rentals")
public class RentalWebController {
    private static final int MAX_CUSTOM_ITEMS = 10;
    private final RentalOrderService rentalOrderService;
    private final CampingPackageDraftService campingPackageDraftService;
    private final RentalService rentalService;
    private final EquipmentService equipmentService;
    private final CategoryService categoryService;

    public RentalWebController(RentalOrderService rentalOrderService,
            CampingPackageDraftService campingPackageDraftService, RentalService rentalService,
            EquipmentService equipmentService, CategoryService categoryService) {
        this.rentalOrderService = rentalOrderService;
        this.campingPackageDraftService = campingPackageDraftService;
        this.rentalService = rentalService;
        this.equipmentService = equipmentService;
        this.categoryService = categoryService;
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
        if (binder instanceof ExtendedServletRequestDataBinder extendedBinder) {
            extendedBinder.setHeaderPredicate(header -> false);
        }
    }

    @InitBinder("customRentalDraftForm")
    public void bindCustomForm(WebDataBinder binder) {
        binder.setAllowedFields("customerId", "rentalDate", "expectedReturnDate", "items[*].equipmentId", "items[*].quantity");
        binder.setAutoGrowCollectionLimit(MAX_CUSTOM_ITEMS);
        if (binder instanceof ExtendedServletRequestDataBinder extendedBinder) {
            extendedBinder.setHeaderPredicate(header -> false);
        }
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
        rejectUnexpectedFields(errors, "Package equipment and quantities are defined by the server.");
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

    @GetMapping("/custom/new")
    public String customNewForm(Model model) throws SQLException {
        if (!model.containsAttribute("customRentalDraftForm")) {
            CustomRentalDraftForm form = new CustomRentalDraftForm();
            form.setItems(new ArrayList<>(List.of(new CustomRentalItemForm())));
            model.addAttribute("customRentalDraftForm", form);
        }
        model.addAttribute("equipmentOptions", equipmentService.list());
        model.addAttribute("categories", categoryService.list());
        return "rentals/custom-new";
    }

    @PostMapping("/custom")
    public String createCustom(@ModelAttribute("customRentalDraftForm") CustomRentalDraftForm form, BindingResult errors,
            Model model, RedirectAttributes redirect, HttpServletResponse response) throws SQLException {
        rejectUnexpectedFields(errors, "Only equipment IDs and quantities may be submitted for a custom rental.");
        if (hasSparseItems(form.getItems())) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            errors.reject("malformedItems", "Equipment item rows must use contiguous indices.");
            return customNewForm(model);
        }
        validateCustom(form, errors);
        if (errors.hasErrors()) return customNewForm(model);
        try {
            List<RentalService.RentalRequestItem> items = form.getItems().stream()
                    .map(item -> new RentalService.RentalRequestItem(item.getEquipmentId(), item.getQuantity()))
                    .toList();
            RentalOrder order = rentalService.createDraft(new RentalService.RentalRequest(form.getCustomerId().trim(),
                    form.getRentalDate(), form.getExpectedReturnDate(), items));
            redirect.addFlashAttribute("success", "Custom rental draft created successfully.");
            return "redirect:/rentals/" + order.getId();
        } catch (IllegalArgumentException exception) {
            errors.reject("invalidRentalDraft", exception.getMessage());
        } catch (SQLException exception) {
            errors.reject("saveFailed", "Unable to create the rental draft. Please try again.");
        }
        return customNewForm(model);
    }

    @ExceptionHandler(InvalidPropertyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String rejectMalformedCustomBinding(Model model) {
        model.addAttribute("message", "The request could not be completed.");
        return "error";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable("id") String id, RedirectAttributes redirect) {
        return lifecycle(id, "Rental confirmed.", rentalService::confirmRental, redirect);
    }

    @PostMapping("/{id}/rent")
    public String rent(@PathVariable("id") String id, RedirectAttributes redirect) {
        return lifecycle(id, "Rental marked as rented.", rentalService::rentRental, redirect);
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable("id") String id, RedirectAttributes redirect) {
        return lifecycle(id, "Rental cancelled.", rentalService::cancelRental, redirect);
    }

    @PostMapping("/{id}/return")
    public String returnRental(@PathVariable("id") String id, RedirectAttributes redirect) {
        return lifecycle(id, "Rental returned.", rentalId -> rentalService.returnRental(rentalId, LocalDate.now()), redirect);
    }

    private String lifecycle(String id, String successMessage, LifecycleAction action, RedirectAttributes redirect) {
        try {
            action.apply(id);
            redirect.addFlashAttribute("success", successMessage);
        } catch (IllegalArgumentException | IllegalStateException | SQLException exception) {
            redirect.addFlashAttribute("error", "Rental lifecycle action could not be completed.");
        }
        return "redirect:/rentals/" + id;
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

    private void validateCustom(CustomRentalDraftForm form, BindingResult errors) {
        if (form.getCustomerId() == null || form.getCustomerId().isBlank()) errors.rejectValue("customerId", "required", "Customer ID is required.");
        LocalDate rentalDate = form.getRentalDate();
        LocalDate expectedReturnDate = form.getExpectedReturnDate();
        if (rentalDate == null) errors.rejectValue("rentalDate", "required", "Rental date is required.");
        if (expectedReturnDate == null) errors.rejectValue("expectedReturnDate", "required", "Expected return date is required.");
        else if (rentalDate != null && expectedReturnDate.isBefore(rentalDate)) errors.rejectValue("expectedReturnDate", "invalidDateRange", "Expected return date cannot be before rental date.");
        List<CustomRentalItemForm> items = form.getItems();
        if (items == null || items.isEmpty()) {
            errors.reject("itemsRequired", "Choose at least one equipment item.");
            return;
        }
        if (items.size() > MAX_CUSTOM_ITEMS) {
            errors.reject("tooManyItems", "A custom rental can contain at most " + MAX_CUSTOM_ITEMS + " items.");
            return;
        }
        for (int index = 0; index < items.size(); index++) {
            CustomRentalItemForm item = items.get(index);
            if (item == null) {
                errors.reject("invalidItems", "Each equipment row must be complete.");
                continue;
            }
            String prefix = "items[" + index + "]";
            if (item.getEquipmentId() == null || item.getEquipmentId().isBlank()) errors.rejectValue(prefix + ".equipmentId", "required", "Equipment is required.");
            if (item.getQuantity() == null && errors.getFieldError(prefix + ".quantity") == null) errors.rejectValue(prefix + ".quantity", "required", "Quantity is required.");
            else if (item.getQuantity() != null && item.getQuantity() <= 0) errors.rejectValue(prefix + ".quantity", "positive", "Quantity must be positive.");
        }
    }

    private static boolean hasSparseItems(List<CustomRentalItemForm> items) {
        boolean emptyRowSeen = false;
        if (items == null) return false;
        for (CustomRentalItemForm item : items) {
            boolean emptyRow = item == null || ((item.getEquipmentId() == null || item.getEquipmentId().isBlank())
                    && item.getQuantity() == null);
            if (emptyRow) emptyRowSeen = true;
            else if (emptyRowSeen) return true;
        }
        return false;
    }

    private void rejectUnexpectedFields(BindingResult errors, String message) {
        if (errors.getSuppressedFields().length > 0) errors.reject("unsupportedFields", message);
    }

    private static String stateName(RentalOrder order) {
        return RentalOrderStatus.fromState(order.getCurrentState()).name();
    }

    @FunctionalInterface
    private interface LifecycleAction {
        RentalOrder apply(String id) throws SQLException;
    }
}
