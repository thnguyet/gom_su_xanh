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
import org.gomsu.orderservice.entity.*;
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

//    @Transactional
//    public OrderResponse createOrder(Long customerId, OrderRequest orderRequest) {
//        // Lay thong tin User (address, phoneNumber)
//        UserResponse user = userClient.getMyInfor();
//
//        // Lay gio hang
//        CartResponse cart = cartService.getCartByCustomerId(customerId);
//
//        List<CartResponse.CartItemResponse> selectedItems = cart.getCartItemsResponse().stream()
//                .filter(item -> orderRequest.getSelectedCartItemIds().contains(item.getProductId()))
//                .toList();
//
//        if (selectedItems.isEmpty()) {
//            throw new RuntimeException("Vui lòng chọn ít nhất một sản phẩm để đặt hàng!");
//        }
//
//        // Lấy phí ship từ ShippingMethod
//        Double shippingFee = shippingMethodService.getEntityById(orderRequest.getShippingMethodId()).getShippingFee();
//
//        // 4. Tính lại tổng tiền chỉ cho những món đã chọn
//        Double subTotal = selectedItems.stream()
//                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
//                .sum();
//
//        Order order = Order.builder()
//                .customerId(customerId)
//                .address(orderRequest.getAddress() != null ? orderRequest.getAddress() : user.getAddress())
//                .phoneNumber(orderRequest.getPhoneNumber() != null ? orderRequest.getPhoneNumber() : user.getPhone())
//                .note(orderRequest.getNote())
//                .status(OrderStatus.PENDING)
//                .totalAmount(subTotal + shippingFee)
//                .paymentMethod(paymentMethodService.getEntityById(orderRequest.getPaymentMethodId()))
//                .shippingMethod(shippingMethodService.getEntityById(orderRequest.getShippingMethodId()))
//                .build();
//
//        // Chuyen doi tu CartItem sang OrderDetail(Luu snapshot gia va ten san pham)
//        List<OrderDetail> details = selectedItems.stream() // Dùng luôn list đã lọc!
//                .map(item -> OrderDetail.builder()
//                        .productId(item.getProductId())
//                        .productName(item.getProductName())
//                        .quantity(item.getQuantity())
//                        .priceAtPurchase(item.getUnitPrice())
//                        .order(order)
//                        .build())
//                .toList();
//
//        // Gan danh sach chi tiet vao Order
//        order.setOrderDetails(details);
//
//        // 1. Chuẩn bị dữ liệu trừ kho
//        List<ProductRestockRequest> updateRequests = selectedItems.stream()
//                .map(item -> new ProductRestockRequest(item.getProductId(), item.getQuantity()))
//                .toList();
//
//        // 2. GỌI FEIGN CLIENT TRỪ KHO
//        // Nếu Product Service báo lỗi (hết hàng), Transaction này sẽ Rollback, Order sẽ không được lưu.
//        productClient.reduceStock(updateRequests);
//
//        // Luu Order xuong DB
//        Order savedOrder = orderRepository.save(order);
//
//        // Thay vì xóa cả Cart, ta chỉ dọn sạch các món hàng bên trong
//        cartRepository.deleteSelectedItems(cart.getId(), orderRequest.getSelectedCartItemIds());
//
//        return toOrderResponse(savedOrder);
//    }

    @Transactional
    public OrderResponse createOrder(Long customerId, OrderRequest orderRequest) {
        // 1. Lấy thông tin User (để lấy địa chỉ/SĐT mặc định)
        UserResponse user = userClient.getMyInfor();

        // 2. Lấy giỏ hàng hiện tại
        CartResponse cart = cartService.getCartByCustomerId(customerId);

        // 3. Lọc ra các sản phẩm khách đã tích chọn trên giao diện
        List<CartResponse.CartItemResponse> selectedItems = cart.getCartItemsResponse().stream()
                .filter(item -> orderRequest.getSelectedCartItemIds().contains(item.getProductId()))
                .toList();

        if (selectedItems.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất một sản phẩm để đặt hàng!");
        }

        // 4. Lấy thông tin Vận chuyển & Thanh toán (Lấy 1 lần để tối ưu)
        ShippingMethod shippingMethod = shippingMethodService.getEntityById(orderRequest.getShippingMethodId());
        PaymentMethod paymentMethod = paymentMethodService.getEntityById(orderRequest.getPaymentMethodId());
        Double shippingFee = shippingMethod.getShippingFee();

        // 5. Tính toán tiền hàng
        Double subTotal = selectedItems.stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                .sum();

        // 6. Xác định địa chỉ và SĐT (Ưu tiên Request -> Sau đó đến Profile User)
        String finalAddress = (orderRequest.getAddress() != null && !orderRequest.getAddress().isBlank())
                ? orderRequest.getAddress() : user.getAddress();
        String finalPhone = (orderRequest.getPhoneNumber() != null && !orderRequest.getPhoneNumber().isBlank())
                ? orderRequest.getPhoneNumber() : user.getPhone();

        if (finalAddress == null || finalAddress.isBlank()) {
            throw new RuntimeException("Địa chỉ nhận hàng không được để trống!");
        }

        // 7. Khởi tạo đối tượng Order
        Order order = Order.builder()
                .customerId(customerId)
                .customerName(user.getUsername()) // Lưu tên khách hàng
                .address(finalAddress)
                .phoneNumber(finalPhone)
                .note(orderRequest.getNote())
                .status(OrderStatus.PENDING)
                .totalAmount(subTotal + shippingFee)
                .paymentMethod(paymentMethod)
                .shippingMethod(shippingMethod)
                .build();

        // 8. Tạo chi tiết đơn hàng (Snapshot tên và giá tại thời điểm mua)
        List<OrderDetail> details = selectedItems.stream()
                .map(item -> OrderDetail.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .priceAtPurchase(item.getUnitPrice())
                        .order(order)
                        .build())
                .toList();
        order.setOrderDetails(details);

        // 9. Gọi Product Service để trừ kho (Nếu hết hàng sẽ tự động Rollback)
        List<ProductRestockRequest> updateRequests = selectedItems.stream()
                .map(item -> new ProductRestockRequest(item.getProductId(), item.getQuantity()))
                .toList();
        productClient.reduceStock(updateRequests);

        // 10. Lưu đơn hàng
        // Sau khi lưu, savedOrder sẽ có ID và các trường createdAt/updatedAt từ BaseEntity
        Order savedOrder = orderRepository.save(order);

        // 11. Dọn dẹp các món đã mua khỏi giỏ hàng
        cartRepository.deleteSelectedItems(cart.getId(), orderRequest.getSelectedCartItemIds());

        // 12. Trả về Response
        return toOrderResponse(savedOrder, user.getUsername());
    }

    public record RestockMessage(Long orderId, List<ProductRestockRequest> requests) {}

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

        return toOrderResponse(updatedOrder, null);
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
            String keyword, Long customerId, OrderStatus status,
            LocalDateTime startDate, LocalDateTime endDate,
            int page, int size, String sortBy, String sortDir) {
 
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        return orderRepository.findAllOrdersForAdmin(keyword, customerId, status, startDate, endDate, pageable)
                .map(order -> toOrderResponse(order, null));
    }
 
    // 3. Hàm cho User: Tái sử dụng luôn hàm của Admin, cực gọn!
    public Page<OrderResponse> getMyOrders(Long customerId, OrderStatus status, int page, int size, String sortBy, String sortDir) {
        // Gọi luôn hàm Admin, truyền startDate và endDate là null
        return getAllOrdersForAdmin(null, customerId, status, null, null, page, size, sortBy, sortDir);
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

        return toOrderResponse(updatedOrder, null);
    }

    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + orderId));
        return toOrderResponse(order, null);
    }

    private OrderResponse toOrderResponse(Order order, String providedCustomerName) {
        String customerName = providedCustomerName;
        if (customerName == null) {
            customerName = order.getCustomerName(); // Lấy tên đã lưu trong Order
        }
        
        // Nếu vẫn null (đơn hàng cũ), mới gọi Identity Service
        if (customerName == null) {
            customerName = "N/A";
            try {
                UserResponse user = userClient.getUserById(order.getCustomerId());
                if (user != null) customerName = user.getUsername();
            } catch (Exception e) {
                // Log error or ignore
            }
        }

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
                .customerName(customerName)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .address(order.getAddress())
                .phoneNumber(order.getPhoneNumber())
                .note(order.getNote())
                .paymentMethod(order.getPaymentMethod() != null ?
                        order.getPaymentMethod().getName() : "N/A")
                .shippingMethod(order.getShippingMethod() != null ?
                        order.getShippingMethod().getName() : "N/A")
                .shippingFee(order.getShippingMethod() != null ?
                        order.getShippingMethod().getShippingFee() : 0.0)
                .orderDetails(detailsResponse)
                .build();
    }
}

