package org.gomsu.productservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gomsu.productservice.dto.ProductRestockMessage;
import org.gomsu.productservice.dto.ReviewUpdateEvent;
import org.gomsu.productservice.dto.request.ProductRestockRequest;
import org.gomsu.productservice.service.ProductService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductMessageListener {

    private final ProductService productService;

    @RabbitListener(queues = "product-restock-queue")
    public void handleRestock(ProductRestockMessage message) { // Nhận Object mới
        if (message == null || message.getRequests() == null) {
            log.warn(">>> Nhận được tin nhắn trống, bỏ qua!");
            return;
        }

        log.info(">>> Nhận tín hiệu hoàn kho cho Đơn hàng ID: {} ({} sản phẩm)",
                message.getOrderId(), message.getRequests().size());

        try {
            // Nguyệt có thể lưu log vào DB ở đây: "Đơn hàng này đang được hoàn kho..."
            productService.restockProducts(message.getRequests());
            log.info(">>> Hoàn kho thành công cho đơn hàng: {}", message.getOrderId());
        } catch (Exception e) {
            log.error(">>> Lỗi nghiêm trọng khi hoàn kho cho đơn {}: {}",
                    message.getOrderId(), e.getMessage());
            // Quăng lỗi ra để RabbitMQ biết mà đẩy vào dlq (Dead Letter Queue) nếu có cấu hình
            throw e;
        }
    }

    // --- 2. NGHIỆP VỤ CẬP NHẬT RATING (Bổ sung cho đồng bộ Review) ---
    @RabbitListener(queues = "product-review-queue") // Tên queue Nguyệt cấu hình trong RabbitConfig
    public void handleReviewUpdate(ReviewUpdateEvent event) {
        if (event == null || event.getProductId() == null) {
            log.warn(">>> Nhận sự kiện Review trống, bỏ qua!");
            return;
        }

        log.info(">>> Đang cập nhật Rating cho SP ID {}: {} sao ({} lượt đánh giá)",
                event.getProductId(), event.getAverageRating(), event.getReviewCount());

        try {
            // Hàm này Nguyệt sẽ viết trong ProductService để update DB
            productService.updateProductRating(
                    event.getProductId(),
                    event.getAverageRating(),
                    event.getReviewCount()
            );
            log.info(">>> Cập nhật Rating thành công cho SP ID: {}", event.getProductId());
        } catch (Exception e) {
            log.error(">>> Lỗi cập nhật Rating cho SP {}: {}", event.getProductId(), e.getMessage());
            // Có thể quăng lỗi để retry nếu cần
        }
    }
}