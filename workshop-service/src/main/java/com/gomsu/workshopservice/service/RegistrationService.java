package com.gomsu.workshopservice.service;

import com.gomsu.workshopservice.client.UserClient;
import com.gomsu.workshopservice.configuration.RabbitMQConfig;
import com.gomsu.workshopservice.dto.response.RegistrationResponse;
import com.gomsu.workshopservice.dto.response.UserResponse;
import com.gomsu.workshopservice.entity.RegistrationStatus;
import com.gomsu.workshopservice.entity.Workshop;
import com.gomsu.workshopservice.entity.WorkshopImage;
import com.gomsu.workshopservice.entity.WorkshopRegistration;
import com.gomsu.workshopservice.repository.RegistrationRepository;
import com.gomsu.workshopservice.repository.WorkshopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {
    private final WorkshopRepository workshopRepository;
    private final RegistrationRepository registrationRepository;
    private final UserClient userClient;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public RegistrationResponse registerWorkshop(Long userId, Long workshopId, Integer quantity, String note) {
        log.info("Bắt đầu đăng ký Workshop ID: {} cho User ID: {}", workshopId, userId);

        // 1. Tìm Workshop
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Workshop!"));

        // 2. Kiểm tra thời gian đăng ký
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(workshop.getRegistrationStartDate()) || now.isAfter(workshop.getRegistrationEndDate())) {
            throw new RuntimeException("Thời gian đăng ký không hợp lệ (chưa mở hoặc đã kết thúc)!");
        }

        // 3. Kiểm tra số lượng vé còn lại
        Integer soldTickets = registrationRepository.countSoldTicketsByWorkshopId(workshopId);
        if (soldTickets + quantity > workshop.getMaxParticipants()) {
            throw new RuntimeException("Workshop đã hết chỗ! Chỉ còn lại " + (workshop.getMaxParticipants() - soldTickets) + " chỗ.");
        }

        // 4. Gọi Identity Service lấy thông tin User (Username, Email...)
        UserResponse user = userClient.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("Không tìm thấy thông tin người dùng từ Identity Service!");
        }

        // 5. Lưu thông tin đăng ký vào Database
        WorkshopRegistration registration = WorkshopRegistration.builder()
                .workshop(workshop)
                .customerId(userId)
                .ticketQuantity(quantity)
                .note(note)
                .status(RegistrationStatus.PENDING)
                .build();

        // Lưu xuống DB (Hàm @PrePersist trong Entity sẽ tự tính totalPrice)
        WorkshopRegistration savedReg = registrationRepository.save(registration);

        // 6. Gửi thông báo qua RabbitMQ (Async)
        sendNotificationToRabbitMQ(user, workshop, quantity);

        log.info("Đăng ký thành công! ID Đăng ký: {}", savedReg.getId());

        // 7. Trả về Response đầy đủ cho FE
        return toResponse(savedReg, user);
    }

    private void sendNotificationToRabbitMQ(UserResponse user, Workshop workshop, Integer quantity) {
        Map<String, Object> emailEvent = new HashMap<>();
        emailEvent.put("userEmail", user.getEmail());
        emailEvent.put("userName", user.getUsername());
        emailEvent.put("workshopName", workshop.getName());
        emailEvent.put("quantity", quantity);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.WORKSHOP_EXCHANGE,
                RabbitMQConfig.REGISTRATION_ROUTING_KEY,
                emailEvent
        );
        log.info("Đã bắn Event thông báo lên RabbitMQ cho User: {}", user.getUsername());
    }

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(page, size, sort);
    }

    public Page<RegistrationResponse> getMyRegistrations(
            Long userId, RegistrationStatus status, String keyword,
            LocalDateTime fromDate, LocalDateTime toDate,
            int page, int size, String sortBy, String sortDir
    ) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<WorkshopRegistration> registrations = registrationRepository.findAllByFilter(userId, status, keyword, fromDate, toDate, pageable);
        UserResponse user = userClient.getUserById(userId);
        return registrations.map(registration -> toResponse(registration, user));
    }

    private RegistrationResponse toResponse(WorkshopRegistration registration, UserResponse user) {
        // Cách hay dùng nhất: Nếu không có ảnh thì trả về null để FE tự xử lý
        String workshopImg = registration.getWorkshop().getImages().stream()
                .findFirst()
                .map(WorkshopImage::getImageUrl)
                .orElse(null);

        return RegistrationResponse.builder()
                .id(registration.getId())
                .customerId(registration.getCustomerId())
                .customerName(user.getUsername())

                .workshopId(registration.getWorkshop().getId())
                .workshopName(registration.getWorkshop().getName())
                .workshopImage(workshopImg)
                .location(registration.getWorkshop().getLocation())
                .workshopStartDate(registration.getWorkshop().getStartDate())
                .workshopEndDate(registration.getWorkshop().getEndDate())

                .pricePerTicket(BigDecimal.valueOf(registration.getWorkshop().getPrice()))
                .ticketQuantity(registration.getTicketQuantity())
                .totalPrice(BigDecimal.valueOf(registration.getTotalPrice()))

                .status(registration.getStatus().name())
                .registrationDate(registration.getRegistrationDate())
                .note(registration.getNote())
                .message("Chào " + user.getUsername() + ", đơn đăng ký của bạn đã được tiếp nhận thành công!")
                .build();
    }
}
