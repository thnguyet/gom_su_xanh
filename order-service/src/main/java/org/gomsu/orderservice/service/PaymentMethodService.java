package org.gomsu.orderservice.service;

import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.dto.request.PaymentMethodRequest;
import org.gomsu.orderservice.dto.response.PaymentMethodResponse;
import org.gomsu.orderservice.entity.PaymentMethod;
import org.gomsu.orderservice.repository.PaymentMethodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    // Chỉ lấy những phương thức đang hoạt động (active = true) để hiển thị cho khách
    public List<PaymentMethodResponse> getAllActivePaymentMethods() {
        return paymentMethodRepository.findAll().stream()
                .filter(PaymentMethod::isActive)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Dùng nội bộ cho OrderService (Vẫn cho phép lấy cả cái inactive để xem đơn hàng cũ)
    public PaymentMethod getEntityById(Long id) {
        return paymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phương thức thanh toán ID: " + id));
    }

    @Transactional
    public PaymentMethodResponse createPaymentMethod(PaymentMethodRequest request) {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setName(request.getName());
        paymentMethod.setActive(true); // Luôn mặc định là true khi tạo mới
        return toResponse(paymentMethodRepository.save(paymentMethod));
    }

    @Transactional
    public PaymentMethodResponse updatePaymentMethod(Long id, PaymentMethodRequest request) {
        PaymentMethod paymentMethod = getEntityById(id);
        paymentMethod.setName(request.getName());
        return toResponse(paymentMethodRepository.save(paymentMethod));
    }

    @Transactional
    public void softDeletePaymentMethod(Long id) {
        PaymentMethod paymentMethod = getEntityById(id);
        paymentMethod.setActive(false); // Xóa mềm: chỉ chuyển trạng thái
        paymentMethodRepository.save(paymentMethod);
    }

    private PaymentMethodResponse toResponse(PaymentMethod entity) {
        return PaymentMethodResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}