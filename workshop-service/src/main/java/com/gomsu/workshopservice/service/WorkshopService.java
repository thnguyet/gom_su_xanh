package com.gomsu.workshopservice.service;

import com.gomsu.workshopservice.client.UserClient;
import com.gomsu.workshopservice.dto.request.WorkshopRequest;
import com.gomsu.workshopservice.dto.response.WorkshopResponse;
import com.gomsu.workshopservice.entity.Workshop;
import com.gomsu.workshopservice.entity.WorkshopImage;
import com.gomsu.workshopservice.repository.WorkshopImageRepository;
import com.gomsu.workshopservice.repository.WorkshopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkshopService {
    private final WorkshopRepository workshopRepository;
    private final CloudinaryService cloudinaryService;
    private final WorkshopImageRepository workshopImageRepository;
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

    private WorkshopResponse toWorkshopResponse(Workshop workshop) {
        List<String> imageUrls = workshop.getImages().stream()
                .map(image -> image.getImageUrl())
                .toList();

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
}