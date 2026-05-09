package com.gomsu.workshopservice.service;

import com.gomsu.workshopservice.client.UserClient;
import com.gomsu.workshopservice.configuration.RabbitMQConfig;
import com.gomsu.workshopservice.dto.response.RegistrationResponse;
import com.gomsu.workshopservice.dto.response.UserResponse;
import com.gomsu.workshopservice.entity.RegistrationStatus;
import com.gomsu.workshopservice.entity.Workshop;
import com.gomsu.workshopservice.entity.WorkshopImage;
import com.gomsu.workshopservice.entity.WorkshopRegistration;
import com.gomsu.workshopservice.exception.AppException;
import com.gomsu.workshopservice.exception.ErrorCode;
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
import java.time.LocalDate;
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
    public RegistrationResponse registerWorkshop(Long userId, Long workshopId, Integer quantity, String note, LocalDate participationDate, String participationTime, String name, String phone) {
        log.info("Bắt đầu đăng ký Workshop ID: {} cho User ID: {}", workshopId, userId);

        // 1. Tìm Workshop
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new AppException(ErrorCode.WORKSHOP_NOT_FOUND));

        if (quantity == null || quantity <= 0) {
            throw new AppException(ErrorCode.REGISTRATION_INVALID_QUANTITY);
        }

        // 2. Kiểm tra thời gian đăng ký
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(workshop.getRegistrationStartDate()) || now.isAfter(workshop.getRegistrationEndDate())) {
            throw new AppException(ErrorCode.REGISTRATION_TIME_INVALID);
        }

        // BỔ SUNG: Chặn đăng ký nếu Workshop đang bị ẩn (active = false)
        if (Boolean.FALSE.equals(workshop.getActive())) {
            throw new AppException(ErrorCode.WORKSHOP_INACTIVE);
        }

        // 3. Kiểm tra số lượng vé còn lại
        int updatedRows = workshopRepository.updateParticipants(workshopId, quantity);
        if (updatedRows == 0) {
            throw new AppException(ErrorCode.REGISTRATION_OUT_OF_SLOTS);
        }

        // 4. Gọi Identity Service lấy thông tin User (Username, Email...)
        UserResponse user = userClient.getMyInfor();
        if (user == null) {
            throw new AppException(ErrorCode.REGISTRATION_USER_NOT_FOUND);
        }

        // Bổ sung validate: Nếu có truyền phone mới thì kiểm tra định dạng
        if (phone != null && !phone.isBlank() && !phone.matches("^(\\+84|0)\\d{9,10}$")) {
            throw new AppException(ErrorCode.REGISTRATION_INVALID_PHONE);
        }

        // 5. Lưu thông tin đăng ký vào Database
        WorkshopRegistration registration = WorkshopRegistration.builder()
                .workshop(workshop)
                .customerId(userId)
                .customerName(name != null && !name.isBlank() ? name : user.getUsername())
                .customerPhone(phone != null && !phone.isBlank() ? phone : user.getPhone())
                .customerEmail(user.getEmail()) // Mặc định lấy từ Profile, không cho override
                .ticketQuantity(quantity)
                .note(note)
                .participationDate(participationDate)
                .participationTime(participationTime)
                .status(RegistrationStatus.CONFIRMED)
                .build();

        // Lưu xuống DB (Hàm @PrePersist trong Entity sẽ tự tính totalPrice)
        WorkshopRegistration savedReg = registrationRepository.save(registration);

        // 6. Gửi thông báo qua RabbitMQ (Async)
        sendNotificationToRabbitMQ(registration.getCustomerName(), registration.getCustomerEmail(), workshop, quantity);

        log.info("Đăng ký thành công! ID Đăng ký: {}", savedReg.getId());

        // 7. Trả về Response đầy đủ cho FE
        return toResponse(savedReg, user);
    }

    private void sendNotificationToRabbitMQ(String userName, String userEmail, Workshop workshop, Integer quantity) {
        try {
            Map<String, Object> emailEvent = new HashMap<>();
            emailEvent.put("userEmail", userEmail);
            emailEvent.put("userName", userName);
            emailEvent.put("workshopName", workshop.getName());
            emailEvent.put("quantity", quantity);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.WORKSHOP_EXCHANGE,
                    RabbitMQConfig.REGISTRATION_ROUTING_KEY,
                    emailEvent
            );
            log.info("Đã bắn Event thông báo lên RabbitMQ cho User: {}", userName);
        } catch (Exception e) {
            log.error("Không thể gửi thông báo RabbitMQ (có thể server RabbitMQ chưa chạy): {}", e.getMessage());
            // Không ném lỗi ra ngoài để tránh rollback transaction đăng ký thành công
        }
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
        Page<WorkshopRegistration> registrations = registrationRepository.findAllByFilter(
                userId, status, keyword, fromDate, toDate, pageable);
        
        // Cố gắng gọi userClient một lần, nếu lỗi thì bỏ qua (dùng fallback)
        UserResponse user = null;
        try {
            user = userClient.getMyInfor();
        } catch(Exception e) {
            log.warn("Không thể lấy thông tin user hiện tại từ Identity Service");
        }
        UserResponse finalUser = user;
        return registrations.map(registration -> toResponse(registration, finalUser));
    }

    public Page<RegistrationResponse> getAllRegistrations(
            RegistrationStatus status, String keyword,
            LocalDateTime fromDate, LocalDateTime toDate,
            int page, int size, String sortBy, String sortDir
    ) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<WorkshopRegistration> registrations = registrationRepository.findAllAdminByFilter(
                status, keyword, fromDate, toDate, pageable);
        return registrations.map(registration -> toResponse(registration, null));
    }

    @Transactional
    public void cancelRegistration(Long registrationId, Long userId) {
        // 1. Tìm bản ghi đăng ký
        WorkshopRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));

        // 2. Kiểm tra quyền sở hữu (tránh việc user A hủy vé của user B)
        if (!registration.getCustomerId().equals(userId)) {
            throw new AppException(ErrorCode.REGISTRATION_UNAUTHORIZED);
        }

        // 3. Kiểm tra trạng thái (chỉ hủy khi đơn đang ở trạng thái CONFIRMED)
        if (registration.getStatus() != RegistrationStatus.CONFIRMED) {
            throw new AppException(ErrorCode.REGISTRATION_ALREADY_CANCELLED);
        }

        // 4. LOGIC QUAN TRỌNG: Kiểm tra điều kiện trước 3 ngày
        // Lấy thời gian bắt đầu Workshop
        LocalDateTime workshopStartTime = registration.getWorkshop().getStartDate();
        LocalDateTime now = LocalDateTime.now();

        // Nếu thời gian hiện tại đã vượt quá (thời gian bắt đầu - 3 ngày)
        if (now.isAfter(workshopStartTime.minusDays(3))) {
            throw new AppException(ErrorCode.REGISTRATION_CANCEL_DEADLINE);
        }

        // 5. Cập nhật trạng thái đơn hàng
        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);

        // 6. Hoàn lại số lượng vé vào kho Workshop
        int updatedRows = workshopRepository.decreaseParticipants(
                registration.getWorkshop().getId(),
                registration.getTicketQuantity()
        );

        if (updatedRows == 0) {
            throw new AppException(ErrorCode.REGISTRATION_REFUND_FAILED);
        }

        log.info("User {} đã hủy thành công đơn đăng ký {}. Vé đã được hoàn lại kho.", userId, registrationId);
    }

    @Transactional
    public void checkInRegistration(Long registrationId) {
        WorkshopRegistration workshopRegistration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));

        if (workshopRegistration.getStatus() != RegistrationStatus.CONFIRMED) {
            throw new AppException(ErrorCode.REGISTRATION_INVALID_CHECKIN);
        }

        workshopRegistration.setStatus(RegistrationStatus.COMPLETED);
        registrationRepository.save(workshopRegistration);

        log.info("Admin đã check-in thành công cho đơn đăng ký ID: {}", registrationId);

    }

    @Transactional(readOnly = true)
    public RegistrationResponse getRegistrationById(Long registrationId) {
        WorkshopRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));
        return toResponse(registration, null);
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
                .customerName(registration.getCustomerName())
                .customerPhone(registration.getCustomerPhone())
                .customerEmail(registration.getCustomerEmail())

                .workshopId(registration.getWorkshop().getId())
                .workshopName(registration.getWorkshop().getName())
                .workshopImage(workshopImg)
                .location(registration.getWorkshop().getLocation())
                .workshopStartDate(registration.getWorkshop().getStartDate())
                .workshopEndDate(registration.getWorkshop().getEndDate())

                .pricePerTicket(BigDecimal.valueOf(registration.getWorkshop().getPrice()))
                .ticketQuantity(registration.getTicketQuantity())
                .totalPrice(registration.getTotalPrice())

                .status(registration.getStatus().name())
                .registrationDate(registration.getRegistrationDate())
                .participationDate(registration.getParticipationDate())
                .participationTime(registration.getParticipationTime())
                .note(registration.getNote())
                .message("Chào " + (user != null ? user.getUsername() : registration.getCustomerName()) + ", đơn đăng ký của bạn đã được tiếp nhận thành công!")
                .build();
    }
}
