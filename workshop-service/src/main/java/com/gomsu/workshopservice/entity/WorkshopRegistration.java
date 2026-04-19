package com.gomsu.workshopservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workshop_registrations")
@Getter // Dùng Getter/Setter thay cho @Data để an toàn hơn với JPA
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
    @ToString.Exclude // Ngăn lỗi vòng lặp khi in Log
    private Workshop workshop;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Integer ticketQuantity;

    private Double totalPrice;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    // FE sẽ dùng cái này để đổi màu trạng thái (Vàng/Xanh/Đỏ)
    @Enumerated(EnumType.STRING)
    private RegistrationStatus status;

    @Column(columnDefinition = "TEXT")
    private String note;

    @PrePersist
    protected void onCreate() {
        this.registrationDate = LocalDateTime.now();
        if (this.status == null) {
            this.status = RegistrationStatus.PENDING;
        }
        // Tính tổng tiền dựa trên giá của Workshop liên kết
        if (this.workshop != null && this.totalPrice == null) {
            this.totalPrice = this.workshop.getPrice() * this.ticketQuantity;
        }
    }
}