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

        // BỔ SUNG: Chặn đăng ký nếu Workshop đang bị ẩn (active = false)
        if (Boolean.FALSE.equals(workshop.getActive())) {
            throw new RuntimeException("Workshop này hiện không còn hoạt động hoặc đã bị đóng!");
        }

        // 3. Kiểm tra số lượng vé còn lại
        int updatedRows = workshopRepository.updateParticipants(workshopId, quantity);
        if (updatedRows == 0) {
            throw new RuntimeException("Workshop đã hết chỗ hoặc số lượng đăng ký vượt quá giới hạn!");
        }

        // 4. Gọi Identity Service lấy thông tin User (Username, Email...)
        UserResponse user = userClient.getMyInfor();
        if (user == null) {
            throw new RuntimeException("Không tìm thấy thông tin người dùng từ Identity Service!");
        }

        // 5. Lưu thông tin đăng ký vào Database
        WorkshopRegistration registration = WorkshopRegistration.builder()
                .workshop(workshop)
                .customerId(userId)
                .customerName(user.getUsername())
                .ticketQuantity(quantity)
                .note(note)
                .status(RegistrationStatus.CONFIRMED)
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
        try {
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đăng ký!"));

        // 2. Kiểm tra quyền sở hữu (tránh việc user A hủy vé của user B)
        if (!registration.getCustomerId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hủy đơn đăng ký này!");
        }

        // 3. Kiểm tra trạng thái (chỉ hủy khi đơn đang ở trạng thái CONFIRMED)
        if (registration.getStatus() != RegistrationStatus.CONFIRMED) {
            throw new RuntimeException("Đơn hàng này đã bị hủy hoặc đã hoàn thành, không thể hủy thêm.");
        }

        // 4. LOGIC QUAN TRỌNG: Kiểm tra điều kiện trước 3 ngày
        // Lấy thời gian bắt đầu Workshop
        LocalDateTime workshopStartTime = registration.getWorkshop().getStartDate();
        LocalDateTime now = LocalDateTime.now();

        // Nếu thời gian hiện tại đã vượt quá (thời gian bắt đầu - 3 ngày)
        if (now.isAfter(workshopStartTime.minusDays(3))) {
            throw new RuntimeException("Đã quá hạn hủy vé! Bạn chỉ có thể hủy trước ngày diễn ra ít nhất 3 ngày.");
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
            throw new RuntimeException("Có lỗi xảy ra khi hoàn trả số lượng vé!");
        }

        log.info("User {} đã hủy thành công đơn đăng ký {}. Vé đã được hoàn lại kho.", userId, registrationId);
    }

    @Transactional
    public void checkInRegistration(Long registrationId) {
        WorkshopRegistration workshopRegistration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đăng ký!"));

        if (workshopRegistration.getStatus() != RegistrationStatus.CONFIRMED) {
            throw new RuntimeException("Đơn đăng ký không hợp lệ để check-in");
        }

        workshopRegistration.setStatus(RegistrationStatus.COMPLETED);
        registrationRepository.save(workshopRegistration);

        log.info("Admin đã check-in thành công cho đơn đăng ký ID: {}", registrationId);

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
                .customerName(user != null ? user.getUsername() : registration.getCustomerName())

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
                .note(registration.getNote())
                .message("Chào " + (user != null ? user.getUsername() : registration.getCustomerName()) + ", đơn đăng ký của bạn đã được tiếp nhận thành công!")
                .build();
    }
}
