package com.campinggearrental.model;

import java.math.BigDecimal;

/**
 * Thiết bị cắm trại + logic số lượng tồn kho (Inventory).
 *
 * Quy tắc: BR04 (giá > 0), BR05 (số lượng không âm),
 *          BR06 (available <= total).
 *
 * Việc GỌI reserve()/release() thuộc về luồng Confirm/Cancel/Return
 * (phần C thông qua EquipmentService); class này chỉ đảm bảo số lượng
 * luôn hợp lệ.
 */
public class Equipment {
    private String equipmentId;
    private String name;
    private String categoryId;
    private BigDecimal pricePerDay;
    private int totalQuantity;
    private int availableQuantity;
    private EquipmentStatus status;

    /** Tạo thiết bị mới: ban đầu available = total. */
    public Equipment(String equipmentId, String name, String categoryId,
                     BigDecimal pricePerDay, int totalQuantity) {
        this(equipmentId, name, categoryId, pricePerDay,
                totalQuantity, totalQuantity, EquipmentStatus.AVAILABLE);
    }

    /** Dùng khi đọc từ database (Repository). */
    public Equipment(String equipmentId, String name, String categoryId,
                     BigDecimal pricePerDay, int totalQuantity,
                     int availableQuantity, EquipmentStatus status) {
        if (equipmentId == null || equipmentId.isBlank()) {
            throw new IllegalArgumentException("Mã thiết bị không được rỗng");
        }
        if (totalQuantity < 0) {
            throw new IllegalArgumentException("Tổng số lượng không được âm");
        }
        if (availableQuantity < 0 || availableQuantity > totalQuantity) {
            throw new IllegalArgumentException(
                    "Số lượng khả dụng phải trong khoảng 0..totalQuantity");
        }
        this.equipmentId = equipmentId.trim();
        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
        setName(name);
        setCategoryId(categoryId);
        setPricePerDay(pricePerDay);
        setStatus(status);
    }

    // ----------------------- Inventory logic -----------------------

    /** Đủ hàng để giữ chưa? (dùng khi Confirm - BR10, BR11) */
    public boolean hasEnoughStock(int quantity) {
        return quantity > 0 && quantity <= availableQuantity;
    }

    /** Giữ hàng khi Confirm (BR12): available -= quantity. */
    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải > 0");
        }
        if (quantity > availableQuantity) {
            throw new IllegalStateException("Không đủ thiết bị '" + name
                    + "': cần " + quantity + ", còn " + availableQuantity);
        }
        availableQuantity -= quantity;
    }

    /** Hoàn hàng khi Cancel Confirmed (BR13) hoặc Return (BR15). */
    public void release(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải > 0");
        }
        if (availableQuantity + quantity > totalQuantity) {
            throw new IllegalStateException(
                    "Hoàn kho vượt quá tổng số lượng của '" + name + "'");
        }
        availableQuantity += quantity;
    }

    /**
     * Đổi tổng số lượng (khi sửa Equipment). Phần chênh lệch được cộng/trừ
     * vào available để số đang giữ/đang thuê không bị sai lệch.
     */
    public void adjustTotalQuantity(int newTotal) {
        if (newTotal < 0) {
            throw new IllegalArgumentException("Tổng số lượng không được âm");
        }
        int newAvailable = availableQuantity + (newTotal - totalQuantity);
        if (newAvailable < 0) {
            throw new IllegalStateException(
                    "Không thể giảm tổng số lượng thấp hơn số đang được giữ/thuê ("
                            + (totalQuantity - availableQuantity) + ")");
        }
        this.totalQuantity = newTotal;
        this.availableQuantity = newAvailable;
    }

    // ------------------------ getters/setters ------------------------

    public String getEquipmentId() {
        return equipmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên thiết bị không được rỗng");
        }
        this.name = name.trim();
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("Danh mục không hợp lệ");
        }
        this.categoryId = categoryId.trim();
    }

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(BigDecimal pricePerDay) {
        if (pricePerDay == null || pricePerDay.signum() <= 0) {
            throw new IllegalArgumentException("Giá thuê phải > 0");
        }
        this.pricePerDay = pricePerDay;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = (status == null) ? EquipmentStatus.AVAILABLE : status;
    }

    @Override
    public String toString() {
        return equipmentId + " - " + name + " (" + availableQuantity + "/" + totalQuantity + ")";
    }
}
