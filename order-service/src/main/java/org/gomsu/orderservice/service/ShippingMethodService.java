package org.gomsu.orderservice.service;

import lombok.RequiredArgsConstructor;
import org.gomsu.orderservice.dto.request.ShippingMethodRequest;
import org.gomsu.orderservice.dto.response.ShippingMethodResponse;
import org.gomsu.orderservice.entity.ShippingMethod;
import org.gomsu.orderservice.repository.ShippingMethodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShippingMethodService {

    private final ShippingMethodRepository shippingMethodRepository;

    public List<ShippingMethodResponse> getAllActiveShippingMethods() {
        return shippingMethodRepository.findAll().stream()
                .filter(ShippingMethod::isActive)
                .map(this::toResponse)
                .collect(Collectors.toList());
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
    public ShippingMethodResponse updateShippingMethod(Long id, ShippingMethodRequest request) {
        ShippingMethod shippingMethod = getEntityById(id);
        shippingMethod.setName(request.getName());
        shippingMethod.setShippingFee(request.getShippingFee());
        return toResponse(shippingMethodRepository.save(shippingMethod));
    }

    @Transactional
    public void softDeleteShippingMethod(Long id) {
        ShippingMethod shippingMethod = getEntityById(id);
        shippingMethod.setActive(false);
        shippingMethodRepository.save(shippingMethod);
    }

    private ShippingMethodResponse toResponse(ShippingMethod entity) {
        return ShippingMethodResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .shippingFee(entity.getShippingFee())
                .build();
    }
}