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
@Table(name = "shipping_methods")
public class ShippingMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "shippingFee")
    private Double shippingFee;

    @OneToMany(mappedBy = "shippingMethod")
    @ToString.Exclude
    @JsonIgnore
    private List<Order> orders = new ArrayList<>();
}
