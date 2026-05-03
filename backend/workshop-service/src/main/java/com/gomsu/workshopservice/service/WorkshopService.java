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
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
                .slug(toSlug(request.getName()))
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
                .active(true)
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
            Boolean isAdmin, // Đổi sang Boolean để khớp với Repo
            Boolean active,
            int page, int size, String sortBy, String sortDir) {

        // 1. Kiểm tra isAdmin null thì mặc định là false (User)
        Boolean adminFlag = (isAdmin != null && isAdmin);

        // 2. Tạo Pageable
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // 3. Gọi Repo với câu Query đã có logic "(:isAdmin IS TRUE OR w.active IS TRUE)"
        Page<Workshop> workshops = workshopRepository.findAllForUserAndAdmin(
                keyword,
                location,
                minPrice,
                maxPrice,
                fromDate,
                toDate,
                adminFlag,
                active,
                pageable);

        return workshops.map(this::toWorkshopResponse);
    }

    public WorkshopResponse getWorkshopById(Long id) {
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Workshop!"));
        return toWorkshopResponse(workshop);
    }

    public WorkshopResponse getWorkshopBySlug(String slug) {
        Workshop workshop = workshopRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Workshop!"));
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
            workshop.setSlug(toSlug(workshopUpdateRequest.getName()));
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

        // Bổ sung cập nhật trạng thái Active (Chỉ update nếu có truyền giá trị mới)
        if (workshopUpdateRequest.getActive() != null) {
            workshop.setActive(workshopUpdateRequest.getActive());
        }

        // 2. Cập nhật thời gian
        if (workshopUpdateRequest.getStartDate() != null) workshop.setStartDate(workshopUpdateRequest.getStartDate());
        if (workshopUpdateRequest.getEndDate() != null) workshop.setEndDate(workshopUpdateRequest.getEndDate());
        if (workshopUpdateRequest.getRegistrationStartDate() != null) workshop.setRegistrationStartDate(workshopUpdateRequest.getRegistrationStartDate());
        if (workshopUpdateRequest.getRegistrationEndDate() != null) workshop.setRegistrationEndDate(workshopUpdateRequest.getRegistrationEndDate());

        // 3. Cập nhật thông tin chi tiết
        if (workshopUpdateRequest.getBenefits() != null) workshop.setBenefits(workshopUpdateRequest.getBenefits());
        if (workshopUpdateRequest.getTools() != null) workshop.setTools(workshopUpdateRequest.getTools());
        if (workshopUpdateRequest.getTargetAudience() != null) workshop.setTargetAudience(workshopUpdateRequest.getTargetAudience());

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

    @Transactional
    public void deleteWorkshop(Long id) {
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Workshop!"));

        // Xóa tất cả ảnh trên Cloudinary trước
        if (workshop.getImages() != null) {
            for (WorkshopImage img : workshop.getImages()) {
                try {
                    cloudinaryService.deleteImage(img.getImageUrl());
                } catch (IOException e) {
                    log.error("Cảnh báo: Không thể xóa ảnh trên mây của Workshop {}: {}", id, e.getMessage());
                }
            }
        }

        // Thực hiện xóa vĩnh viễn khỏi Database
        workshopRepository.delete(workshop);
        log.info(">>> Đã xóa vĩnh viễn Workshop ID: {} khỏi Database.", id);
    }

    // Xem so luong nguoi tham gia cua 1 workshop
    public WorkshopAttendeeResponse getAttendeesWithFilter(
            Long workshopId, RegistrationStatus status, String keyword,
            LocalDateTime fromDate, LocalDateTime toDate,
            int page, int size, String sortBy, String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<WorkshopRegistration> registrationPage = registrationRepository.findAttendeesByFilter(
                workshopId, status, keyword, fromDate, toDate, pageable);

        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new RuntimeException("Workshop không tồn tại"));

        // Thay vì dùng userCache và gọi Client, mình truyền null vào toResponse
        // vì toResponse đã được mình sửa để ưu tiên lấy Name từ bản ghi Registration rồi.
        Page<RegistrationResponse> attendeePage = registrationPage.map(reg -> toResponse(reg, null));

        return WorkshopAttendeeResponse.builder()
                .workshopId(workshopId)
                .workshopName(workshop.getName())
                .maxParticipants(workshop.getMaxParticipants())
                .currentParticipants(workshop.getCurrentParticipants())
                .attendees(attendeePage)
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
        List<WorkshopResponse.ImageInfo> imagesInfo = (workshop.getImages() != null)
                ? workshop.getImages().stream()
                .map(img -> WorkshopResponse.ImageInfo.builder()
                        .id(img.getId())
                        .url(img.getImageUrl())
                        .build())
                .toList()
                : new ArrayList<>();

        List<String> imageUrls = imagesInfo.stream().map(WorkshopResponse.ImageInfo::getUrl).toList();

        // Dùng orElse(null) thay vì .get() để an toàn hơn Nguyệt nhé
        String mainImg = imageUrls.stream().findFirst().orElse(null);

        return WorkshopResponse.builder()
                .id(workshop.getId())
                .name(workshop.getName())
                .slug(toSlug(workshop.getName()))
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
                .imagesInfo(imagesInfo)
                .active(workshop.getActive())
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
                .customerName(reg.getCustomerName() != null ? reg.getCustomerName() : (user != null ? user.getUsername() : "N/A"))

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

    public String toSlug(String title) {
        if (title == null || title.isBlank()) return "";

        // 1. Chuyển về chữ thường trước để xử lý cho dễ
        String slug = title.toLowerCase();

        // 2. Thay thế các ký tự đặc biệt của tiếng Việt (đ, ý, ...)
        slug = slug.replaceAll("đ", "d");

        // 3. Chuẩn hóa để loại bỏ dấu (Normalizer)
        String normalized = Normalizer.normalize(slug, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        slug = pattern.matcher(normalized).replaceAll("");

        // 4. Xóa các ký tự không phải chữ cái/số, thay khoảng trắng bằng gạch ngang
        return slug.replaceAll("[^a-z0-9\\s]", "") // Xóa ký tự đặc biệt còn sót lại
                .replaceAll("\\s+", "-")           // Thay khoảng trắng thành 1 dấu gạch ngang
                .replaceAll("^-+|-+$", "");        // Xóa dấu gạch ngang dư thừa ở đầu và cuối
    }
}