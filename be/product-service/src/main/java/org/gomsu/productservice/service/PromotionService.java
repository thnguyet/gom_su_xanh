package org.gomsu.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gomsu.productservice.dto.request.ProductCreationRequest;
import org.gomsu.productservice.dto.request.PromotionRequest;
import org.gomsu.productservice.dto.request.PromotionUpdateRequest;
import org.gomsu.productservice.dto.response.PromotionResponse;
import org.gomsu.productservice.entity.Product;
import org.gomsu.productservice.entity.ProductPromotion;
import org.gomsu.productservice.entity.Promotion;
import org.gomsu.productservice.exception.AppException;
import org.gomsu.productservice.exception.ErrorCode;
import org.gomsu.productservice.repository.CategoryRepository;
import org.gomsu.productservice.repository.ProductRepository;
import org.gomsu.productservice.repository.PromotionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService {
    private final PromotionRepository promotionRepository;

    private final ProductRepository productRepository; // Cần cái này để tìm Product từ ID

    //Tao promotion moi
    @Transactional
    public PromotionResponse createPromotion(PromotionRequest promotionRequest) {
        String name = promotionRequest.getName().trim();
        String slug = toSlug(name);

        // Check trùng cho chắc chắn nè Nguyệt
        if (promotionRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.PROMOTION_ALREADY_EXISTS);
        }
        Promotion promotion = new Promotion();
        promotion.setName(promotionRequest.getName());
        promotion.setSlug(toSlug(promotionRequest.getName()));
        promotion.setStartDate(promotionRequest.getStartDate());
        promotion.setEndDate(promotionRequest.getEndDate());
        promotion.setIsActive(true);
        List<ProductPromotion> productPromotions = promotionRequest.getProductDiscounts().stream()
                .map(item -> {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
                    ProductPromotion productPromotion = new ProductPromotion();
                    productPromotion.setProduct(product);
                    productPromotion.setDiscountPercentage(item.getDiscountPercentage());
                    productPromotion.setPromotion(promotion);
                    return  productPromotion;
                })
                .collect(Collectors.toList());
        promotion.setProductPromotions(productPromotions);
        return toPromotionResponse(promotionRepository.save(promotion));
    }

    // Sua promotion
    @Transactional
    public PromotionResponse updatePromotion(Long id, PromotionUpdateRequest request) {
        // 1. Kiểm tra sự tồn tại
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        // 2. Cập nhật thông tin cơ bản (Chỉ cập nhật nếu KHÔNG NULL)
        if (request.getName() != null && !request.getName().isBlank()) {
            promotion.setName(request.getName());
            promotion.setSlug(toSlug(request.getName()));
        }

        if (request.getStartDate() != null) {
            promotion.setStartDate(request.getStartDate());
        }

        if (request.getEndDate() != null) {
            promotion.setEndDate(request.getEndDate());
        }

        // 3. Logic MERGE sản phẩm: Cập nhật hoặc Thêm mới, tuyệt đối không .clear()
        if (request.getProductDiscounts() != null && !request.getProductDiscounts().isEmpty()) {

            // Tạo Map để tra cứu nhanh các sản phẩm đang có trong khuyến mãi hiện tại
            Map<Long, ProductPromotion> existingMap = promotion.getProductPromotions().stream()
                    .collect(Collectors.toMap(pp -> pp.getProduct().getId(), pp -> pp));

            for (PromotionRequest.ProductDiscountRequest dto : request.getProductDiscounts()) {
                if (existingMap.containsKey(dto.getProductId())) {
                    // TRƯỜNG HỢP 1: Sản phẩm đã tồn tại -> Chỉ cập nhật lại % giảm giá
                    ProductPromotion existing = existingMap.get(dto.getProductId());
                    existing.setDiscountPercentage(dto.getDiscountPercentage());
                }
                else {
                    // TRƯỜNG HỢP 2: Sản phẩm chưa có trong đợt này -> Tạo mới và thêm vào List
                    Product product = productRepository.findById(dto.getProductId())
                            .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

                    ProductPromotion pp = new ProductPromotion();
                    pp.setProduct(product);
                    pp.setPromotion(promotion);
                    pp.setDiscountPercentage(dto.getDiscountPercentage());

                    // Thêm trực tiếp vào danh sách của promotion (JPA sẽ tự động save bản ghi mới vào bảng trung gian)
                    promotion.getProductPromotions().add(pp);
                }
            }
        }

        // 4. Lưu lại (Hibernate sẽ tự động nhận biết thay đổi ở các Object bên trong List)
        return toPromotionResponse(promotionRepository.save(promotion));
    }

    @Transactional
    public PromotionResponse stopPromotion(Long id) {
        // 1. Tìm chương trình khuyến mãi
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        // 2. Kiểm tra nếu nó đã dừng rồi thì không cần làm gì nữa
        if (Boolean.FALSE.equals(promotion.getIsActive())) {
            throw new AppException(ErrorCode.PROMOTION_ALREADY_STOPPED);
        }

        // 3. Cập nhật trạng thái và thời gian kết thúc
        promotion.setIsActive(false);
        promotion.setEndDate(LocalDateTime.now()); // Kết thúc ngay tại giây này

        // 4. (Quan trọng) Xử lý các sản phẩm đang liên kết
        // Nguyệt nên cân nhắc:
        // Cách A: Xóa sạch các bản ghi giảm giá trong bảng trung gian để Product quay về giá gốc.
        // Cách B: Giữ lại để làm lịch sử nhưng logic lấy giá phải check isActive của Promotion.

        // Ở đây mình chọn cách xóa (Clear) để giải phóng Database và ngắt kết nối giảm giá:
        promotion.getProductPromotions().clear();

        // 5. Lưu lại
        Promotion savedPromotion = promotionRepository.save(promotion);

        log.info("Admin đã dừng khẩn cấp chương trình: {}", savedPromotion.getName());

        return toPromotionResponse(savedPromotion);
    }

    // Lay promotion theo slug
    public PromotionResponse getPromotionBySlug(String slug) {
        Promotion promotion = promotionRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
        return toPromotionResponse(promotion);
    }

    // Hàm xóa Promotion
    @Transactional
    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        // Clear danh sách để Hibernate hiểu là cần xóa các bản ghi con trước
        promotion.getProductPromotions().clear();
        promotionRepository.delete(promotion);
    }

    // Lấy chi tiết một đợt khuyến mãi
    public PromotionResponse getPromotionById(Long id) {
        // Tìm Promotion trong DB, nếu không có thì ném lỗi
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        // Dùng chính cái hàm "phù thủy" toPromotionResponse để biến nó thành DTO đẹp đẽ
        return toPromotionResponse(promotion);
    }

    // Lấy tất cả đợt khuyến mãi (Phân trang)
    public Page<PromotionResponse> getAllPromotions(
            int page,
            int size,
            String keyword,
            String sortBy,
            String sortDir,
            LocalDateTime fromDate,
            LocalDateTime toDate) {

        // 1. Quyết định chiều sắp xếp (Mặc định lấy createdAt từ BaseEntity)
        String actualSortBy = (sortBy == null || sortBy.equals("id")) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(actualSortBy).ascending()
                : Sort.by(actualSortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // 2. Gọi Repository để search tổng hợp (Giống Product)
        Page<Promotion> promotionPage = promotionRepository.searchPromotions(
                keyword != null ? keyword.trim() : null,
                fromDate,
                toDate,
                pageable);

        return promotionPage.map(this::toPromotionResponse);
    }

    public PromotionResponse toPromotionResponse(Promotion promotion) {
        List<PromotionResponse.ProductPromotionItemResponse> items = promotion.getProductPromotions()
                .stream()
                .map(pp -> {
                    Product product = pp.getProduct();
                    Long productId = product.getId();
                    String productName = product.getName();
                    Double discountPercentage = pp.getDiscountPercentage();
                    Double originalPrice = product.getPrice();
                    Double discountedPrice = originalPrice * ( 1 - (discountPercentage / 100));
                    return PromotionResponse.ProductPromotionItemResponse.builder()
                            .productId(productId)
                            .productName(productName)
                            .productSlug(product.getSlug())
                            .discountPercentage(discountPercentage)
                            .originalPrice(originalPrice)
                            .discountedPrice(discountedPrice)
                            .build();
                })
                .collect(Collectors.toList());
        return PromotionResponse.builder()
                .id(promotion.getId())
                .name(promotion.getName())
                .slug(promotion.getSlug())
                .isActive(promotion.getIsActive())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .createdAt(promotion.getCreatedAt())
                .updatedAt(promotion.getUpdatedAt())
                .items(items)
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
