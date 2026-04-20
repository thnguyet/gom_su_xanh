package org.gomsu.orderservice.service;

import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.client.ProductClient;
import org.gomsu.orderservice.client.UserClient;
import org.gomsu.orderservice.configuration.RabbitMQConfig;
import org.gomsu.orderservice.dto.request.OrderRequest;
import org.gomsu.orderservice.dto.request.ProductRestockRequest;
import org.gomsu.orderservice.dto.response.CartResponse;
import org.gomsu.orderservice.dto.response.OrderResponse;
import org.gomsu.orderservice.dto.response.UserResponse;
import org.gomsu.orderservice.entity.Order;
import org.gomsu.orderservice.entity.OrderDetail;
import org.gomsu.orderservice.entity.OrderStatus;
import org.gomsu.orderservice.repository.CartRepository;
import org.gomsu.orderservice.repository.OrderRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    final OrderRepository orderRepository;
    final UserClient userClient;
    final ProductClient productClient;
    private final CartService cartService;
    private final CartRepository cartRepository;
    private final PaymentMethodService paymentMethodService;
    private final ShippingMethodService shippingMethodService;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public OrderResponse createOrder(Long customerId, OrderRequest orderRequest) {
        // Lay thong tin User (address, phoneNumber)
        UserResponse user = userClient.getMyInfor();

        // Lay gio hang
        CartResponse cart = cartService.getCartByCustomerId(customerId);

        List<CartResponse.CartItemResponse> selectedItems = cart.getCartItemsResponse().stream()
                .filter(item -> orderRequest.getSelectedCartItemIds().contains(item.getProductId()))
                .toList();

        if (selectedItems.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất một sản phẩm để đặt hàng!");
        }

        // 4. Tính lại tổng tiền chỉ cho những món đã chọn
        Double subTotal = selectedItems.stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                .sum();

        Order order = Order.builder()
                .customerId(customerId)
                .address(orderRequest.getAddress() != null ? orderRequest.getAddress() : user.getAddress())
                .phoneNumber(orderRequest.getPhoneNumber() != null ? orderRequest.getPhoneNumber() : user.getPhone())
                .note(orderRequest.getNote())
                .status(OrderStatus.PENDING)
                .totalAmount(subTotal)
                .paymentMethod(paymentMethodService.getEntityById(orderRequest.getPaymentMethodId()))
                .shippingMethod(shippingMethodService.getEntityById(orderRequest.getShippingMethodId()))
                .build();

        // Chuyen doi tu CartItem sang OrderDetail(Luu snapshot gia va ten san pham)
        List<OrderDetail> details = selectedItems.stream() // Dùng luôn list đã lọc!
                .map(item -> OrderDetail.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .priceAtPurchase(item.getUnitPrice())
                        .order(order)
                        .build())
                .toList();

        // Gan danh sach chi tiet vao Order
        order.setOrderDetails(details);

        // 1. Chuẩn bị dữ liệu trừ kho
        List<ProductRestockRequest> updateRequests = selectedItems.stream()
                .map(item -> new ProductRestockRequest(item.getProductId(), item.getQuantity()))
                .toList();

        // 2. GỌI FEIGN CLIENT TRỪ KHO
        // Nếu Product Service báo lỗi (hết hàng), Transaction này sẽ Rollback, Order sẽ không được lưu.
        productClient.reduceStock(updateRequests);

        // Luu Order xuong DB
        Order savedOrder = orderRepository.save(order);

        // Thay vì xóa cả Cart, ta chỉ dọn sạch các món hàng bên trong
        cartRepository.deleteSelectedItems(cart.getId(), orderRequest.getSelectedCartItemIds());

        return toOrderResponse(savedOrder);
    }

    public record RestockMessage(Long orderId, List<ProductRestockRequest> items) {}

    @Transactional
    public OrderResponse cancelOrder(Long customerId, Long orderId, boolean isAdmin) {
        // Tim don hang
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        // Kiem tra xem don hang co dung la cua nguoi nay khong
        if (!isAdmin && !order.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Bạn không có quyền hủy đơn hàng này!");
        }

        // Chi cho phep huy khi o trang thai PENDING hoac CONFIRMED
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new RuntimeException("Đơn hàng đang trong quá trình vận chuển, không thể hủy!");
        }

        // Cap nhat trang thai CANCELLED
        order.setStatus(OrderStatus.CANCELLED);

        Order updatedOrder = orderRepository.save(order);

        List<ProductRestockRequest> restockRequests = order.getOrderDetails().stream()
                .map(detail -> new ProductRestockRequest(
                        detail.getProductId(),
                        detail.getQuantity()))
                .toList();

        // Gửi vào Exchange với Routing Key đã cấu hình trong RabbitMQConfig
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                new RestockMessage(orderId, restockRequests)
        );

        return toOrderResponse(updatedOrder);
    }

    // 1. Tạo một hàm dùng chung để khởi tạo Pageable (Helper method)
    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(page, size, sort);
    }

    // 2. Hàm cho Admin: Giữ nguyên vì nó là hàm mạnh mẽ nhất
    public Page<OrderResponse> getAllOrdersForAdmin(
            Long customerId, OrderStatus status,
            LocalDateTime startDate, LocalDateTime endDate,
            int page, int size, String sortBy, String sortDir) {

        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        return orderRepository.findAllOrdersForAdmin(customerId, status, startDate, endDate, pageable)
                .map(this::toOrderResponse);
    }

    // 3. Hàm cho User: Tái sử dụng luôn hàm của Admin, cực gọn!
    public Page<OrderResponse> getMyOrders(Long customerId, OrderStatus status, int page, int size, String sortBy, String sortDir) {
        // Gọi luôn hàm Admin, truyền startDate và endDate là null
        return getAllOrdersForAdmin(customerId, status, null, null, page, size, sortBy, sortDir);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus, Long adminId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        // 1. Nếu Admin chọn trạng thái HỦY
        if (newStatus == OrderStatus.CANCELLED) {
            // Gọi luôn hàm cancelOrder có sẵn của Nguyệt (truyền isAdmin = true)
            return this.cancelOrder(adminId, orderId, true);
        }

        // 2. Nếu chuyển sang các trạng thái khác (SHIPPING, COMPLETED, v.v.)
        // Kiểm tra logic: Không cho phép chuyển từ CANCELLED sang các trạng thái khác
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Đơn hàng đã hủy không thể cập nhật trạng thái khác!");
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        return toOrderResponse(updatedOrder);
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderResponse.OrderDetailResponse> detailsResponse = order.getOrderDetails().stream()
                .map(detail -> OrderResponse.OrderDetailResponse.builder()
                        .productId(detail.getProductId())
                        .productName(detail.getProductName())
                        .priceAtPurchase(detail.getPriceAtPurchase())
                        .quantity(detail.getQuantity())
                        .subTotal(detail.getPriceAtPurchase() * detail.getQuantity())
                        .build())
                .toList();
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .orderDate(order.getOrderDate())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .address(order.getAddress())
                .phoneNumber(order.getPhoneNumber())
                .note(order.getNote())
                .paymentMethod(order.getPaymentMethod() != null ?
                        order.getPaymentMethod().getName() : "N/A")
                .shippingAddress(order.getShippingMethod() != null ?
                        order.getShippingMethod().getName() : "N/A")
                .orderDetails(detailsResponse)
                .build();
    }
}

