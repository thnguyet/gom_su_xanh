package org.gomsu.orderservice.entity;

public enum OrderStatus {
    PENDING, // Cho xy ly -> DUOC HUY
    CONFIRMED, // Da xac nhan -> Tuy chinh sach, thuong van huy duoc
    SHIPPING, // Dang giao -> KHONG DUOC HUY
    DELIVERED, // Da gia -> KHONG DUOC HUY
    COMPLETED,
    CANCELLED
}
