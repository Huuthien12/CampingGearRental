package com.campinggearrental.web;

import jakarta.servlet.http.HttpServletResponse;
import com.campinggearrental.factory.CampingPackageFactory;
import com.campinggearrental.factory.CampingPackageCreator;
import com.campinggearrental.factory.CampingPackageItem;
import com.campinggearrental.factory.CampingPackageType;
import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.model.Customer;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.model.RentalOrderStatus;
import com.campinggearrental.service.CampingPackageDraftService;
import com.campinggearrental.service.CategoryService;
import com.campinggearrental.service.CustomerService;
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
    private final CustomerService customerService;

    public RentalWebController(RentalOrderService rentalOrderService,
            CampingPackageDraftService campingPackageDraftService, RentalService rentalService,
            EquipmentService equipmentService, CategoryService categoryService, CustomerService customerService) {
        this.rentalOrderService = rentalOrderService;
        this.campingPackageDraftService = campingPackageDraftService;
        this.rentalService = rentalService;
        this.equipmentService = equipmentService;
        this.categoryService = categoryService;
        this.customerService = customerService;
    }

    @GetMapping
    public String list(Model model) throws SQLException {
        List<RentalOrder> rentals = rentalOrderService.findAll();
        Map<String, String> states = new LinkedHashMap<>();
        rentals.forEach(rental -> states.put(rental.getId(), stateName(rental)));
        model.addAttribute("rentals", rentals);
        model.addAttribute("rentalStates", states);
        model.addAttribute("customerNames", customerNames());
        return "rentals/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") String id, Model model) throws SQLException {
        RentalOrder order = rentalOrderService.findById(id);
        if (order == null) return "error/404";
        model.addAttribute("order", order);
        model.addAttribute("stateName", stateName(order));
        model.addAttribute("customerNames", customerNames());
        model.addAttribute("equipmentNames", equipmentNames());
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
        model.addAttribute("customerOptions", customerOptions());
        Map<String, Equipment> packageEquipment = packageEquipment();
        Map<CampingPackageType, List<PackageItemView>> packageItems = new LinkedHashMap<>();
        for (CampingPackageType type : CampingPackageType.values()) {
            CampingPackageCreator creator = CampingPackageFactory.creatorFor(type);
            List<PackageItemView> items = creator.createPackage().items().stream()
                    .map(item -> packageItemView(item, packageEquipment)).toList();
            packageItems.put(type, items);
        }
        model.addAttribute("packageItems", packageItems);
        return "rentals/new";
    }

    private Map<String, Equipment> packageEquipment() {
        Map<String, Equipment> equipment = new LinkedHashMap<>();
        try {
            for (Equipment item : equipmentService.list()) equipment.put(item.getEquipmentId(), item);
        } catch (SQLException ignored) {
            // Package IDs remain a safe presentation fallback when the catalog cannot be read.
        }
        return equipment;
    }

    private static PackageItemView packageItemView(CampingPackageItem item, Map<String, Equipment> equipmentById) {
        Equipment equipment = equipmentById.get(item.equipmentId());
        String name = equipment == null || equipment.getName().isBlank() ? item.equipmentId() : equipment.getName();
        boolean active = equipment != null && equipment.getStatus() == EquipmentStatus.AVAILABLE;
        boolean sufficient = active && equipment.hasEnoughStock(item.quantity());
        return new PackageItemView(item.equipmentId(), name, item.quantity(),
                equipment == null ? null : equipment.getAvailableQuantity(),
                equipment == null ? null : equipment.getTotalQuantity(), active, sufficient);
    }

    @PostMapping
    public String create(@ModelAttribute("rentalDraftForm") RentalDraftForm form, BindingResult errors,
            Model model, RedirectAttributes redirect) {
        rejectUnexpectedFields(errors, "Thiết bị và số lượng trong gói được máy chủ xác định.");
        validate(form, errors);
        if (errors.hasErrors()) return newForm(model);
        try {
            RentalOrder order = campingPackageDraftService.createDraftFromPackage(form.getCustomerId().trim(),
                    form.getRentalDate(), form.getExpectedReturnDate(), form.getPackageType());
            redirect.addFlashAttribute("success", "Đã tạo đơn thuê nháp theo gói.");
            return "redirect:/rentals/" + order.getId();
        } catch (IllegalArgumentException exception) {
            errors.reject("invalidRentalDraft", exception.getMessage());
        } catch (SQLException exception) {
            errors.reject("saveFailed", "Không thể tạo đơn thuê nháp. Vui lòng thử lại.");
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
        model.addAttribute("customerOptions", customerOptions());
        return "rentals/custom-new";
    }

    private List<Customer> customerOptions() {
        try {
            return customerService.findAll();
        } catch (SQLException ignored) {
            return List.of();
        }
    }

    private Map<String, String> customerNames() throws SQLException {
        Map<String, String> names = new LinkedHashMap<>();
        for (Customer customer : customerService.findAll()) names.put(customer.id(), customer.fullName());
        return names;
    }

    private Map<String, String> equipmentNames() throws SQLException {
        Map<String, String> names = new LinkedHashMap<>();
        for (Equipment item : equipmentService.list()) names.put(item.getEquipmentId(), item.getName());
        return names;
    }

    @PostMapping("/custom")
    public String createCustom(@ModelAttribute("customRentalDraftForm") CustomRentalDraftForm form, BindingResult errors,
            Model model, RedirectAttributes redirect, HttpServletResponse response) throws SQLException {
        rejectUnexpectedFields(errors, "Chỉ được gửi mã thiết bị và số lượng khi thuê từng món.");
        if (hasSparseItems(form.getItems())) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            errors.reject("malformedItems", "Các dòng thiết bị phải có chỉ số liên tiếp.");
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
            redirect.addFlashAttribute("success", "Đã tạo đơn thuê nháp theo từng món.");
            return "redirect:/rentals/" + order.getId();
        } catch (IllegalArgumentException exception) {
            errors.reject("invalidRentalDraft", exception.getMessage());
        } catch (SQLException exception) {
            errors.reject("saveFailed", "Không thể tạo đơn thuê nháp. Vui lòng thử lại.");
        }
        return customNewForm(model);
    }

    @ExceptionHandler(InvalidPropertyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String rejectMalformedCustomBinding(Model model) {
        model.addAttribute("message", "Không thể xử lý yêu cầu này.");
        return "error";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable("id") String id, RedirectAttributes redirect) {
        return lifecycle(id, "Đã xác nhận đơn thuê.", rentalService::confirmRental, redirect);
    }

    @PostMapping("/{id}/rent")
    public String rent(@PathVariable("id") String id, RedirectAttributes redirect) {
        return lifecycle(id, "Đơn thuê đã chuyển sang đang cho thuê.", rentalService::rentRental, redirect);
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable("id") String id, RedirectAttributes redirect) {
        return lifecycle(id, "Đã hủy đơn thuê.", rentalService::cancelRental, redirect);
    }

    @PostMapping("/{id}/return")
    public String returnRental(@PathVariable("id") String id, RedirectAttributes redirect) {
        return lifecycle(id, "Đã ghi nhận trả thiết bị.", rentalId -> rentalService.returnRental(rentalId, LocalDate.now()), redirect);
    }

    private String lifecycle(String id, String successMessage, LifecycleAction action, RedirectAttributes redirect) {
        try {
            action.apply(id);
            redirect.addFlashAttribute("success", successMessage);
        } catch (IllegalArgumentException | IllegalStateException | SQLException exception) {
            redirect.addFlashAttribute("error", "Không thể thực hiện thao tác vòng đời đơn thuê.");
        }
        return "redirect:/rentals/" + id;
    }

    private void validate(RentalDraftForm form, BindingResult errors) {
        if (form.getCustomerId() == null || form.getCustomerId().isBlank()) errors.rejectValue("customerId", "required", "Cần nhập mã khách hàng.");
        LocalDate rentalDate = form.getRentalDate();
        LocalDate expectedReturnDate = form.getExpectedReturnDate();
        if (rentalDate == null) errors.rejectValue("rentalDate", "required", "Cần chọn ngày thuê.");
        if (expectedReturnDate == null) errors.rejectValue("expectedReturnDate", "required", "Cần chọn ngày dự kiến trả.");
        else if (rentalDate != null && expectedReturnDate.isBefore(rentalDate)) errors.rejectValue("expectedReturnDate", "invalidDateRange", "Ngày dự kiến trả không được trước ngày thuê.");
        if (form.getPackageType() == null) errors.rejectValue("packageType", "required", "Cần chọn gói cắm trại.");
    }

    private void validateCustom(CustomRentalDraftForm form, BindingResult errors) {
        if (form.getCustomerId() == null || form.getCustomerId().isBlank()) errors.rejectValue("customerId", "required", "Cần nhập mã khách hàng.");
        LocalDate rentalDate = form.getRentalDate();
        LocalDate expectedReturnDate = form.getExpectedReturnDate();
        if (rentalDate == null) errors.rejectValue("rentalDate", "required", "Cần chọn ngày thuê.");
        if (expectedReturnDate == null) errors.rejectValue("expectedReturnDate", "required", "Cần chọn ngày dự kiến trả.");
        else if (rentalDate != null && expectedReturnDate.isBefore(rentalDate)) errors.rejectValue("expectedReturnDate", "invalidDateRange", "Ngày dự kiến trả không được trước ngày thuê.");
        List<CustomRentalItemForm> items = form.getItems();
        if (items == null || items.isEmpty()) {
            errors.reject("itemsRequired", "Cần chọn ít nhất một thiết bị.");
            return;
        }
        if (items.size() > MAX_CUSTOM_ITEMS) {
            errors.reject("tooManyItems", "Đơn thuê từng món có tối đa " + MAX_CUSTOM_ITEMS + " thiết bị.");
            return;
        }
        for (int index = 0; index < items.size(); index++) {
            CustomRentalItemForm item = items.get(index);
            if (item == null) {
                errors.reject("invalidItems", "Mỗi dòng thiết bị phải được nhập đầy đủ.");
                continue;
            }
            String prefix = "items[" + index + "]";
            if (item.getEquipmentId() == null || item.getEquipmentId().isBlank()) errors.rejectValue(prefix + ".equipmentId", "required", "Cần chọn thiết bị.");
            if (item.getQuantity() == null && errors.getFieldError(prefix + ".quantity") == null) errors.rejectValue(prefix + ".quantity", "required", "Cần nhập số lượng.");
            else if (item.getQuantity() != null && item.getQuantity() <= 0) errors.rejectValue(prefix + ".quantity", "positive", "Số lượng phải lớn hơn 0.");
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

    public record PackageItemView(String equipmentId, String name, int quantity, Integer availableQuantity,
                                  Integer totalQuantity, boolean active, boolean sufficient) { }

    @FunctionalInterface
    private interface LifecycleAction {
        RentalOrder apply(String id) throws SQLException;
    }
}
