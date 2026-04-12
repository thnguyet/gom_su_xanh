package org.gomsu.orderservice.service;

import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.client.UserClient;
import org.gomsu.orderservice.dto.request.OrderRequest;
import org.gomsu.orderservice.dto.response.CartResponse;
import org.gomsu.orderservice.dto.response.OrderResponse;
import org.gomsu.orderservice.dto.response.UserResponse;
import org.gomsu.orderservice.entity.Cart;
import org.gomsu.orderservice.entity.Order;
import org.gomsu.orderservice.entity.OrderDetail;
import org.gomsu.orderservice.entity.OrderStatus;
import org.gomsu.orderservice.repository.CartRepository;
import org.gomsu.orderservice.repository.OrderRepository;
import org.gomsu.orderservice.repository.PaymentMethodRepository;
import org.gomsu.orderservice.repository.ShippingMethodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    final OrderRepository orderRepository;
    final UserClient userClient;
    private final CartService cartService;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ShippingMethodRepository shippingMethodRepository;
    private final CartRepository cartRepository;
    private final PaymentMethodService paymentMethodService;
    private final ShippingMethodService shippingMethodService;

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

        // Luu Order xuong DB
        Order savedOrder = orderRepository.save(order);

        // Thay vì xóa cả Cart, ta chỉ dọn sạch các món hàng bên trong
        cartRepository.deleteSelectedItems(cart.getId(), orderRequest.getSelectedCartItemIds());

        return toOrderResponse(savedOrder);
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

