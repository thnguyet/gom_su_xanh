package org.gomsu.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.gomsu.productservice.dto.request.ProductRestockRequest;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRestockMessage {
    private Long orderId; // Để biết tin nhắn này của đơn hàng nào
    private List<ProductRestockRequest> requests;
}
