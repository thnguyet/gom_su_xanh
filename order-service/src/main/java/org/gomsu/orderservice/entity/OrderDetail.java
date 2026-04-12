package org.gomsu.orderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_details")
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id")
    private Long productId;

    private String productName;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "price_at_purchase")
    private Double priceAtPurchase;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
