package org.gomsu.productservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gomsu.productservice.dto.request.ProductRestockRequest;
import org.gomsu.productservice.service.ProductService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j // Dùng để in log cho chuyên nghiệp
public class ProductMessageListener {

    private final ProductService productService;

    // Tên queue phải khớp với cái Nguyệt đặt trong RabbitMQConfig
    @RabbitListener(queues = "product-restock-queue")
    public void handleRestock(List<ProductRestockRequest> requests) {
        log.info(">>> Nhận được tín hiệu hoàn kho từ RabbitMQ cho {} sản phẩm", requests.size());

        try {
            productService.restockProducts(requests);
            log.info(">>> Hoàn kho thành công!");
        } catch (Exception e) {
            log.error(">>> Lỗi khi hoàn kho: {}", e.getMessage());
            // Nếu lỗi, tin nhắn sẽ được xử lý lại tùy theo cấu hình RabbitMQ
        }
    }
}