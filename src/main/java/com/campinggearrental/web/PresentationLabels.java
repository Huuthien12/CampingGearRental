package com.campinggearrental.web;

import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalOrderStatus;
import com.campinggearrental.factory.CampingPackageType;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Presentation-only Vietnamese labels; persisted values and business rules stay unchanged. */
@Component("presentationLabels")
public class PresentationLabels {
    private static final DateTimeFormatter VIETNAMESE_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
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

    public String packageType(CampingPackageType value) { return value == null ? "" : packageType(value.name()); }

    public String packageType(String value) {
        if (value == null) return "";
        return switch (value) {
            case "SOLO" -> "Một người";
            case "COUPLE" -> "Cặp đôi";
            case "FAMILY" -> "Gia đình";
            default -> value;
        };
    }

    public String money(BigDecimal value) {
        if (value == null) return "";
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        int fractionDigits = Math.max(0, value.stripTrailingZeros().scale());
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(fractionDigits);
        return formatter.format(value) + " ₫";
    }

    public String rentalId(String value) {
        if (value == null) return "";
        return value.length() <= 12 ? value : value.substring(0, 8) + "…";
    }

    public String date(LocalDate value) {
        return value == null ? "" : VIETNAMESE_DATE.format(value);
    }

    public String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
