package com.campinggearrental.model;

/**
 * Trạng thái của thiết bị trong danh mục (lưu ở cột equipment.status).
 * AVAILABLE = đang cho thuê bình thường (khớp seed.sql của nhóm).
 * INACTIVE  = ngừng cho thuê; dùng thay cho xóa cứng để không vỡ
 *             khóa ngoại với rental_details.
 */
public enum EquipmentStatus {
    AVAILABLE,
    INACTIVE
}
