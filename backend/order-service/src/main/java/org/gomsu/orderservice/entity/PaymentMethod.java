package org.gomsu.orderservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_methods")
public class PaymentMethod extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "payment_code")
    private String paymentCode;

    @OneToMany(mappedBy = "paymentMethod")
    @ToString.Exclude
    @JsonIgnore
    private List<Order> orders = new ArrayList<>();

    private Boolean active = true;
}
