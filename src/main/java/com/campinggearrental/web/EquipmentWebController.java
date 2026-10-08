package com.campinggearrental.web;

import com.campinggearrental.model.Category;
import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.service.CategoryService;
import com.campinggearrental.service.EquipmentService;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/equipment")
public class EquipmentWebController {
    private final EquipmentService equipmentService;
    private final CategoryService categoryService;

    public EquipmentWebController(EquipmentService equipmentService, CategoryService categoryService) {
        this.equipmentService = equipmentService;
        this.categoryService = categoryService;
    }

    @InitBinder("equipmentForm")
    public void bindForm(WebDataBinder binder, NativeWebRequest request) {
        binder.setAllowedFields("equipmentId", "name", "categoryId", "pricePerDay", "totalQuantity", "status");
        if ("/equipment/{id}/edit".equals(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                NativeWebRequest.SCOPE_REQUEST))) {
            binder.setRequiredFields("status");
        }
    }

    @GetMapping
    public String list(@RequestParam(name = "q", defaultValue = "") String query, Model model) throws SQLException {
        List<Category> categories = categoryService.list();
        Map<String, String> names = new LinkedHashMap<>();
        categories.forEach(category -> names.put(category.getCategoryId(), category.getName()));
        model.addAttribute("equipment", equipmentService.search(query));
        model.addAttribute("categoryNames", names);
        model.addAttribute("q", query);
        return "equipment/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) throws SQLException {
        model.addAttribute("equipmentForm", new EquipmentForm());
        return form(model, null);
    }

    @PostMapping
    public String create(@ModelAttribute("equipmentForm") EquipmentForm input, BindingResult errors,
                         Model model, RedirectAttributes redirect) throws SQLException {
        rejectSuppressedFields(errors);
        if (!errors.hasErrors()) {
            try {
                Equipment equipment = input.toNewEquipment();
                validateCategory(equipment.getCategoryId(), errors);
                if (!errors.hasErrors()) {
                    equipmentService.create(equipment);
                    redirect.addFlashAttribute("success", "Đã thêm thiết bị.");
                    return "redirect:/equipment";
                }
            } catch (IllegalArgumentException | IllegalStateException exception) {
                errors.reject("invalidEquipment", exception.getMessage());
            } catch (SQLException exception) {
                errors.reject("saveFailed", "Không thể lưu thiết bị. Hãy kiểm tra mã thiết bị và thử lại.");
            }
        }
        return form(model, null);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") String id, Model model) throws SQLException {
        Equipment equipment = equipmentService.getById(id);
        model.addAttribute("equipmentForm", EquipmentForm.from(equipment));
        return form(model, equipment);
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable("id") String id, @ModelAttribute("equipmentForm") EquipmentForm input,
                       BindingResult errors, Model model, RedirectAttributes redirect) throws SQLException {
        Equipment current = equipmentService.getById(id);
        input.setEquipmentId(current.getEquipmentId());
        rejectSuppressedFields(errors);
        if (!errors.hasErrors()) {
            try {
                EquipmentService.CatalogUpdate command = input.toCatalogUpdate();
                validateCategory(command.categoryId(), errors);
                if (!errors.hasErrors()) {
                    equipmentService.updateCatalog(id, command);
                    redirect.addFlashAttribute("success", "Đã cập nhật thiết bị.");
                    return "redirect:/equipment";
                }
            } catch (IllegalArgumentException | IllegalStateException exception) {
                errors.reject("invalidEquipment", exception.getMessage());
            } catch (SQLException exception) {
                errors.reject("saveFailed", "Không thể lưu thiết bị. Vui lòng thử lại.");
            }
        }
        return form(model, current);
    }

    private void validateCategory(String id, BindingResult errors) throws SQLException {
        if (categoryService.findById(id).isEmpty()) {
            errors.rejectValue("categoryId", "unknownCategory", "Hãy chọn một loại thiết bị có sẵn.");
        }
    }

    private void rejectSuppressedFields(BindingResult errors) {
        // MVC also offers request headers to the binder; ignored headers are not invalid form input.
        for (String field : errors.getSuppressedFields()) {
            if ("availableQuantity".equalsIgnoreCase(field)) {
                errors.reject("unsupportedFields", "Không thể chỉnh sửa trực tiếp số lượng sẵn sàng.");
                break;
            }
        }
    }

    private String form(Model model, Equipment current) throws SQLException {
        model.addAttribute("categories", categoryService.list());
        model.addAttribute("statuses", EquipmentStatus.values());
        model.addAttribute("editing", current != null);
        model.addAttribute("current", current);
        return "equipment/form";
    }
}
