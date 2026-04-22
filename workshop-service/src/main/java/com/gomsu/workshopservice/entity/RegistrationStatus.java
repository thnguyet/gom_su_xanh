package com.gomsu.workshopservice.entity;

public enum RegistrationStatus {
    CONFIRMED,  // Đã thanh toán thành công
    CANCELLED,  // Khách hàng hoặc Admin đã hủy
    COMPLETED   // Đã tham gia workshop xong
}