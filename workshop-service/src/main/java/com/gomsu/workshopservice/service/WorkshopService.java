package com.gomsu.workshopservice.service;

import com.gomsu.workshopservice.client.UserClient;
import com.gomsu.workshopservice.dto.request.WorkshopRequest;
import com.gomsu.workshopservice.dto.request.WorkshopUpdateRequest;
import com.gomsu.workshopservice.dto.response.RegistrationResponse;
import com.gomsu.workshopservice.dto.response.UserResponse;
import com.gomsu.workshopservice.dto.response.WorkshopAttendeeResponse;
import com.gomsu.workshopservice.dto.response.WorkshopResponse;
import com.gomsu.workshopservice.entity.RegistrationStatus;
import com.gomsu.workshopservice.entity.Workshop;
import com.gomsu.workshopservice.entity.WorkshopImage;
import com.gomsu.workshopservice.entity.WorkshopRegistration;
import com.gomsu.workshopservice.repository.RegistrationRepository;
import com.gomsu.workshopservice.repository.WorkshopImageRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkshopService {
    private final WorkshopRepository workshopRepository;
    private final CloudinaryService cloudinaryService;
    private final WorkshopImageRepository workshopImageRepository;
    private final RegistrationRepository registrationRepository;
    private final UserClient userClient;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public WorkshopResponse createWorkshop(WorkshopRequest request, List<MultipartFile> images) {
        // 1. Tạo đối tượng Workshop từ Request (Bổ sung các trường mới)
        Workshop workshop = Workshop.builder()
                .name(request.getName())
                .description(request.getDescription())
                .content(request.getContent()) // <--- THÊM DÒNG NÀY
                .location(request.getLocation())
                .price(request.getPrice())
                .maxParticipants(request.getMaxParticipants())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .registrationStartDate(request.getRegistrationStartDate())
                .registrationEndDate(request.getRegistrationEndDate())
                .targetAudience(request.getTargetAudience()) // <--- THÊM DÒNG NÀY
                .tools(request.getTools())                   // <--- THÊM DÒNG NÀY
                .benefits(request.getBenefits())             // <--- THÊM DÒNG NÀY
                .images(new ArrayList<>())
                .build();

        Workshop savedWorkshop = workshopRepository.save(workshop);

        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                try {
                    String imageUrl = cloudinaryService.uploadImage(file);
                    WorkshopImage workImage = WorkshopImage.builder()
                            .imageUrl(imageUrl)
                            .workshop(savedWorkshop)
                            .build();

                    WorkshopImage savedWorkshopImage = workshopImageRepository.save(workImage);
                    savedWorkshop.getImages().add(savedWorkshopImage);
                } catch (IOException e) {
                    throw new RuntimeException("Lỗi khi upload ảnh: " + e.getMessage());
                }
            }
        }

        log.info("Tạo thành công Workshop: {}", savedWorkshop.getName());
        return toWorkshopResponse(savedWorkshop);
    }

    public Page<WorkshopResponse> getWorkshops(
            String keyword, String location,
            Double minPrice, Double maxPrice,
            LocalDateTime fromDate, LocalDateTime toDate,
            boolean isAdmin, // Tham số quyết định quyền xem
            int page, int size, String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Workshop> workshops = workshopRepository.findAllForUserAndAdmin(
                keyword, location, minPrice, maxPrice, fromDate, toDate, isAdmin, pageable);

        return workshops.map(this::toWorkshopResponse);
    }

    public WorkshopResponse getWorkshopById(Long id) {
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Workshop với ID: " + id));

        return toWorkshopResponse(workshop);
    }

    //Them anh cho workshop
    @Transactional
    public WorkshopResponse uploadWorkshopImages(Long id, List<MultipartFile> images) {
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy workshop này"));
        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                try {
                    String imageUrl = cloudinaryService.uploadImage(file);
                    WorkshopImage workshopImage = WorkshopImage.builder()
                            .imageUrl(imageUrl)
                            .workshop(workshop)
                            .build();
                    workshop.getImages().add(workshopImage);
                } catch (IOException e) {
                    throw new RuntimeException("Lỗi khi upload ảnh mới");
                }
            }
            workshopRepository.save(workshop);
        }
        return this.toWorkshopResponse(workshop);
    }

    //Sua workshop
    public WorkshopResponse updateWorkshop(Long id, WorkshopUpdateRequest workshopUpdateRequest, List<MultipartFile> images) {
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Workshop!"));

        if (workshopUpdateRequest.getName() != null && !workshopUpdateRequest.getName().isEmpty()) {
            workshop.setName(workshopUpdateRequest.getName());
        }

        if (workshopUpdateRequest.getPrice() != null && workshopUpdateRequest.getPrice() != 0) {
            workshop.setPrice(workshopUpdateRequest.getPrice());
        }

        if (workshopUpdateRequest.getDescription() != null && !workshopUpdateRequest.getDescription().isEmpty()) {
            workshop.setDescription(workshopUpdateRequest.getDescription());
        }

        if (workshopUpdateRequest.getLocation() != null && !workshopUpdateRequest.getLocation().isEmpty()) {
            workshop.setLocation(workshopUpdateRequest.getLocation());
        }

        if (workshopUpdateRequest.getContent() != null && !workshopUpdateRequest.getContent().isEmpty()) {
            workshop.setContent(workshopUpdateRequest.getContent());
        }

        if (workshopUpdateRequest.getMaxParticipants() != null && workshopUpdateRequest.getMaxParticipants() != 0) {
            workshop.setMaxParticipants(workshopUpdateRequest.getMaxParticipants());
        }

        // 2. Cập nhật thời gian
        if (workshopUpdateRequest.getStartDate() != null) workshop.setStartDate(workshopUpdateRequest.getStartDate());
        if (workshopUpdateRequest.getEndDate() != null) workshop.setEndDate(workshopUpdateRequest.getEndDate());
        if (workshopUpdateRequest.getRegistrationStartDate() != null) workshop.setRegistrationStartDate(workshopUpdateRequest.getRegistrationStartDate());
        if (workshopUpdateRequest.getRegistrationEndDate() != null) workshop.setRegistrationEndDate(workshopUpdateRequest.getRegistrationEndDate());

        // 3. Cập nhật thông tin chi tiết
        if (workshopUpdateRequest.getBenefits() != null) workshop.setBenefits(workshopUpdateRequest.getBenefits());
        if (workshopUpdateRequest.getTools() != null) workshop.setTools(workshopUpdateRequest.getTools());

        // Them anh
        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                try {
                    String url = cloudinaryService.uploadImage(file);
                    workshop.getImages().add(WorkshopImage.builder()
                            .imageUrl(url)
                            .workshop(workshop)
                            .build());
                } catch (IOException e) {
                    throw new RuntimeException("Lỗi upload ảnh mới");
                }
            }
        }

        //Xoa anh
        if (workshopUpdateRequest.getDeletedImageIds() != null && !workshopUpdateRequest.getDeletedImageIds().isEmpty()) {
            workshop.getImages().removeIf(img -> {
                if (workshopUpdateRequest.getDeletedImageIds().contains(img.getId())) {
                    try {
                        cloudinaryService.deleteImage(img.getImageUrl()); // Xóa trên mây
                        return true; // Xóa trong List (Hibernate sẽ tự xóa trong DB nhờ orphanRemoval)
                    } catch (IOException e) {
                        log.error("Lỗi xóa ảnh Cloudinary ID: {}", img.getId());
                    }
                }
                return false;
            });
        }

        return toWorkshopResponse(workshopRepository.save(workshop));
    }

    //Xoa san pham
    @Transactional
    public void deleteWorkshop(Long id) {
        Workshop deleteWorkshop = workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Workshop cần xóa!"));
        if(deleteWorkshop.getImages() != null && !deleteWorkshop.getImages().isEmpty()) {
            for (WorkshopImage workshopImage : deleteWorkshop.getImages()) {
                try {
                    cloudinaryService.deleteImage(workshopImage.getImageUrl());
                } catch (IOException e) {
                    System.err.println("Cảnh báo: Không thể xóa ảnh này trên Cloudinary");
                }
            }
        }
        workshopRepository.deleteById(id);
    }

    // Xem so luong nguoi tham gia cua 1 workshop
    public WorkshopAttendeeResponse getAttendeesWithFilter(
            Long workshopId, RegistrationStatus status, String keyword,
            LocalDateTime fromDate, LocalDateTime toDate,
            int page, int size, String sortBy, String sortDir
    ) {
        // 1. Khởi tạo phân trang
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // 2. Lấy Page dữ liệu từ Repo
        Page<WorkshopRegistration> registrationPage = registrationRepository.findAttendeesByFilter(
                workshopId, status, keyword, fromDate, toDate, pageable);

        // 3. Lấy thông tin Workshop & Số lượng (Dùng các hàm Nguyệt đã có)
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new RuntimeException("Workshop không tồn tại"));
        Integer totalSold = workshop.getCurrentParticipants();

        // 4. Map sang Response (Lấy thông tin User từ Identity Service)
        // Dùng cách dùng Map/Cache để tối ưu n+1 như mình đã nhắc Nguyệt nhé
        Map<Long, UserResponse> userCache = new HashMap<>();
        Page<RegistrationResponse> attendeePage = registrationPage.map(reg -> {
            UserResponse user = userCache.computeIfAbsent(reg.getCustomerId(),
                    id -> userClient.getUserById(id));
            return toResponse(reg, user);
        });

        // 5. Trả về Wrapper chứa Page
        return WorkshopAttendeeResponse.builder()
                .workshopId(workshopId)
                .workshopName(workshop.getName())
                .maxParticipants(workshop.getMaxParticipants())
                .currentParticipants(totalSold)
                .attendees(attendeePage) // Trả về cả cục Page để FE làm phân trang
                .build();
    }

    public Map<String, Object> getWorkshopStatsByWorkshopID(Long id) {
        return registrationRepository.getWorkshopStatsByWorkshopID(id);
    }

    public List<Map<String, Object>> getAllWorkshopsStatistics() {
        List<Map<String, Object>> stats = registrationRepository.getAllWorkshopStats();

        return stats;
    }

    private WorkshopResponse toWorkshopResponse(Workshop workshop) {
        // 1. Kiểm tra danh sách images từ Entity tránh bị Null
        List<String> imageUrls = (workshop.getImages() != null)
                ? workshop.getImages().stream()
                .map(WorkshopImage::getImageUrl)
                .toList()
                : new ArrayList<>(); // Trả về list rỗng thay vì null để FE đỡ bị lỗi map()

        // Dùng orElse(null) thay vì .get() để an toàn hơn Nguyệt nhé
        String mainImg = imageUrls.stream().findFirst().orElse(null);

        return WorkshopResponse.builder()
                .id(workshop.getId())
                .name(workshop.getName())
                .description(workshop.getDescription())
                .content(workshop.getContent())   // <--- THÊM DÒNG NÀY
                .location(workshop.getLocation())
                .price(workshop.getPrice())
                .maxParticipants(workshop.getMaxParticipants())
                .currentParticipants(workshop.getCurrentParticipants())
                .startDate(workshop.getStartDate())
                .endDate(workshop.getEndDate())
                .registrationStartDate(workshop.getRegistrationStartDate())
                .registrationEndDate(workshop.getRegistrationEndDate())
                .targetAudience(workshop.getTargetAudience()) // <--- THÊM DÒNG NÀY
                .tools(workshop.getTools())                   // <--- THÊM DÒNG NÀY
                .benefits(workshop.getBenefits())             // <--- THÊM DÒNG NÀY
                .mainImage(mainImg)
                .allImages(imageUrls)
                .build();
    }

    private RegistrationResponse toResponse(WorkshopRegistration reg, UserResponse user) {
        // 1. Lấy thông tin Workshop từ Entity để map sang DTO
        Workshop workshop = reg.getWorkshop();

        // 2. Tái sử dụng logic lấy ảnh mà Nguyệt đã viết bên toWorkshopResponse
        List<String> imageUrls = (workshop.getImages() != null)
                ? workshop.getImages().stream()
                .map(WorkshopImage::getImageUrl)
                .toList()
                : new ArrayList<>();

        String mainImg = imageUrls.stream().findFirst().orElse(null);

        // 3. Xây dựng Builder hoàn chỉnh
        return RegistrationResponse.builder()
                .id(reg.getId())

                // --- Thông tin khách hàng (Lấy từ UserResponse của Identity Service) ---
                .customerId(reg.getCustomerId())
                .customerName(user != null ? user.getUsername() : "N/A")

                // --- Thông tin Workshop (Lấy từ Entity) ---
                .workshopId(workshop.getId())
                .workshopName(workshop.getName())
                .workshopImage(mainImg)
                .location(workshop.getLocation())
                .workshopStartDate(workshop.getStartDate())
                .workshopEndDate(workshop.getEndDate())

                // --- Thông tin chi tiết vé (Lấy từ bản ghi Registration để đảm bảo tính lịch sử) ---
                .pricePerTicket(reg.getPricePerTicket())
                .ticketQuantity(reg.getTicketQuantity())
                .totalPrice(reg.getTotalPrice())

                // --- Trạng thái và thời gian ---
                .status(reg.getStatus().name())
                .registrationDate(reg.getRegistrationDate())

                // --- Ghi chú từ khách hàng (Trường Nguyệt vừa thêm) ---
                .note(reg.getNote())

                .message("Thành công")
                .build();
    }
}