package com.gomsu.workshopservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workshop_registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_id", nullable = false)
    @ToString.Exclude
    private Workshop workshop;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    private String customerName;

    @Column(nullable = false)
    private Integer ticketQuantity;

    // Đổi sang BigDecimal để tính toán chính xác
    private java.math.BigDecimal pricePerTicket;
    private java.math.BigDecimal totalPrice;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    @Enumerated(EnumType.STRING)
    private RegistrationStatus status;

    @Column(columnDefinition = "TEXT")
    private String note;

    private java.time.LocalDate participationDate;
    private String participationTime;

    @PrePersist
    protected void onCreate() {
        this.registrationDate = LocalDateTime.now();
        if (this.status == null) {
            this.status = RegistrationStatus.CONFIRMED;
        }

        // Logic tính tiền an toàn hơn
        if (this.workshop != null) {
            // Lưu lại giá tại thời điểm mua
            this.pricePerTicket = java.math.BigDecimal.valueOf(this.workshop.getPrice());

            if (this.totalPrice == null) {
                this.totalPrice = this.pricePerTicket.multiply(java.math.BigDecimal.valueOf(this.ticketQuantity));
            }
        }
    }
}