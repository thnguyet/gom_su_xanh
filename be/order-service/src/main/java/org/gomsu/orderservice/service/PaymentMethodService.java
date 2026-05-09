package org.gomsu.orderservice.service;

import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.dto.request.PaymentMethodRequest;
import org.gomsu.orderservice.dto.request.PaymentMethodUpdateRequest;
import org.gomsu.orderservice.dto.response.PaymentMethodResponse;
import org.gomsu.orderservice.dto.response.ShippingMethodResponse;
import org.gomsu.orderservice.entity.PaymentMethod;
import org.gomsu.orderservice.entity.ShippingMethod;
import org.gomsu.orderservice.exception.AppException;
import org.gomsu.orderservice.exception.ErrorCode;
import org.gomsu.orderservice.repository.PaymentMethodRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    // Chỉ lấy những phương thức đang hoạt động (active = true) để hiển thị cho khách
    public Page<PaymentMethodResponse> searchPaymentMethods(
            Boolean onlyActive, String keyword,
            LocalDateTime fromDate, LocalDateTime toDate,
            int page, int size, String sortBy, String sortDir) {

        // Tạo đối tượng sắp xếp động
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return paymentMethodRepository.searchMethods(onlyActive, keyword, fromDate, toDate, pageable)
                .map(this::toResponse);
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
    public PaymentMethodResponse updatePaymentMethod(Long id, PaymentMethodUpdateRequest request) {
        PaymentMethod paymentMethod = getEntityById(id);
        // Kiểm tra tên: không null và không được chỉ toàn dấu cách
        if (request.getName() != null && !request.getName().isBlank()) {
            paymentMethod.setName(request.getName());
        }
        if (request.getActive() != null) {
            paymentMethod.setActive(request.getActive());
        }
        return toResponse(paymentMethodRepository.save(paymentMethod));
    }

    @Transactional
    // Xóa vĩnh viễn khỏi DB
    public void deletePaymentMethod(Long id) {
        if (!paymentMethodRepository.existsById(id)) {
            throw new AppException(ErrorCode.PAYMENT_METHOD_NOT_FOUND);
        }
        paymentMethodRepository.deleteById(id);
    }

    @Transactional
    // Để Admin tạm dừng phương thức thanh toán. Nó vẫn hiện ở bảng Admin để mình còn biết mà Bật lại.
    public PaymentMethodResponse changeStatus(Long id, Boolean active) {
        PaymentMethod paymentMethod = getEntityById(id);
        paymentMethod.setActive(active);
        return toResponse(paymentMethodRepository.save(paymentMethod));
    }

    private PaymentMethodResponse toResponse(PaymentMethod entity) {
        return PaymentMethodResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt()) // Lấy từ BaseEntity
                .updatedAt(entity.getUpdatedAt()) // Lấy từ BaseEntity
                .build();
    }
}