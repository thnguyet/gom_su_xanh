package org.gomsu.orderservice.service;

import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.dto.request.ShippingMethodRequest;
import org.gomsu.orderservice.dto.request.ShippingMethodUpdateRequest;
import org.gomsu.orderservice.dto.response.ShippingMethodResponse;
import org.gomsu.orderservice.entity.ShippingMethod;
import org.gomsu.orderservice.exception.AppException;
import org.gomsu.orderservice.exception.ErrorCode;
import org.gomsu.orderservice.repository.ShippingMethodRepository;
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
public class ShippingMethodService {

    private final ShippingMethodRepository shippingMethodRepository;

    public Page<ShippingMethodResponse> searchShippingMethods(
            Boolean onlyActive, String keyword,
            LocalDateTime fromDate, LocalDateTime toDate,
            int page, int size, String sortBy, String sortDir) {

        // Tạo đối tượng sắp xếp động
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return shippingMethodRepository.searchMethods(onlyActive, keyword, fromDate, toDate, pageable)
                .map(this::toResponse);
    }

    public ShippingMethod getEntityById(Long id) {
        return shippingMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn vị vận chuyển ID: " + id));
    }

    @Transactional
    public ShippingMethodResponse createShippingMethod(ShippingMethodRequest request) {
        ShippingMethod shippingMethod = new ShippingMethod();
        shippingMethod.setName(request.getName());
        shippingMethod.setShippingFee(request.getShippingFee());
        shippingMethod.setActive(true);
        return toResponse(shippingMethodRepository.save(shippingMethod));
    }

    @Transactional
    public ShippingMethodResponse updateShippingMethod(Long id, ShippingMethodUpdateRequest request) {
        ShippingMethod shippingMethod = getEntityById(id);

        // Kiểm tra tên: không null và không được chỉ toàn dấu cách
        if (request.getName() != null && !request.getName().isBlank()) {
            shippingMethod.setName(request.getName());
        }

        // Kiểm tra phí: chỉ set khi request có gửi (khác null)
        if (request.getShippingFee() != null) {
            shippingMethod.setShippingFee(request.getShippingFee());
        }

        // Nếu Nguyệt có thêm trường active trong Request thì cũng làm tương tự
         if (request.getActive() != null) {
            shippingMethod.setActive(request.getActive());
         }

        return toResponse(shippingMethodRepository.save(shippingMethod));
    }

    @Transactional
    // Xóa vĩnh viễn khỏi database theo yêu cầu Admin
    public void deleteShippingMethod(Long id) {
        if (!shippingMethodRepository.existsById(id)) {
                        throw new AppException(ErrorCode.SHIPPING_METHOD_NOT_FOUND);
        }
        shippingMethodRepository.deleteById(id);
    }

    @Transactional
    // Để Admin tạm dừng đơn vị vận chuyển (Ví dụ: "Hôm nay Viettel Post nghỉ lễ, gạt nút Tắt phát là xong"). Nó vẫn hiện ở bảng Admin để mình còn biết mà Bật lại.
    public ShippingMethodResponse changeStatus(Long id, Boolean active) {
        ShippingMethod shippingMethod = getEntityById(id);
        shippingMethod.setActive(active);
        return toResponse(shippingMethodRepository.save(shippingMethod));
    }

    private ShippingMethodResponse toResponse(ShippingMethod entity) {
        return ShippingMethodResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .shippingFee(entity.getShippingFee())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt()) // Lấy từ BaseEntity
                .updatedAt(entity.getUpdatedAt()) // Lấy từ BaseEntity
                .build();
    }
}