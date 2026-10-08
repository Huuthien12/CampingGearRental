package com.campinggearrental.web;

import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalOrderStatus;
import org.springframework.stereotype.Component;

/** Presentation-only Vietnamese labels; persisted values and business rules stay unchanged. */
@Component("presentationLabels")
public class PresentationLabels {
    public String rentalState(RentalOrderStatus value) { return value == null ? "" : rentalState(value.name()); }
    public String rentalState(String value) {
        if (value == null) return "";
        return switch (value) {
            case "PENDING" -> "Chờ xác nhận";
            case "CONFIRMED" -> "Đã xác nhận";
            case "RENTED" -> "Đang cho thuê";
            case "RETURNED" -> "Đã trả";
            case "CANCELLED" -> "Đã hủy";
            default -> value;
        };
    }

    public String paymentStatus(PaymentStatus value) { return value == null ? "" : paymentStatus(value.name()); }
    public String paymentStatus(String value) {
        if (value == null) return "";
        return switch (value) {
            case "UNPAID" -> "Chưa thanh toán";
            case "PAID" -> "Đã thanh toán";
            default -> value;
        };
    }

    public String equipmentStatus(EquipmentStatus value) { return value == null ? "" : equipmentStatus(value.name()); }
    public String equipmentStatus(String value) {
        if (value == null) return "";
        return switch (value) {
            case "AVAILABLE" -> "Sẵn sàng cho thuê";
            case "INACTIVE" -> "Ngừng cho thuê";
            default -> value;
        };
    }

    public String category(String value) {
        if (value == null) return "";
        return switch (value) {
            case "Tent" -> "Lều";
            case "Sleeping Bag" -> "Túi ngủ";
            case "Chair" -> "Ghế";
            case "Lamp" -> "Đèn";
            case "Stove" -> "Bếp";
            default -> value;
        };
    }
}
