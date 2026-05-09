package com.gomsu.workshopservice.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    // --- Thông tin Workshop ---
    private Long workshopId;
    private String workshopName;
    private String workshopImage;
    private String location;
    private LocalDateTime workshopStartDate;
    private LocalDateTime workshopEndDate;

    // --- Thông tin chi tiết vé ---
    private BigDecimal pricePerTicket;
    private Integer ticketQuantity;
    private BigDecimal totalPrice;

    private String status;
    private LocalDateTime registrationDate;

    // --- BỔ SUNG TRƯỜNG NOTE ---
    private String note; // Hiển thị lại ghi chú mà khách đã nhập
    private LocalDate participationDate;
    private String participationTime;

    private String message;
}