package com.gomsu.workshopservice.entity;

public enum RegistrationStatus {
    PENDING,    // Đang chờ thanh toán/xác nhận
    CONFIRMED,  // Đã thanh toán thành công
    CANCELLED,  // Khách hàng hoặc Admin đã hủy
    COMPLETED   // Đã tham gia workshop xong
}