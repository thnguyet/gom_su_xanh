package org.gomsu.productservice.service;

import lombok.RequiredArgsConstructor;
import org.gomsu.productservice.dto.request.ProductCreationRequest;
import org.gomsu.productservice.dto.request.PromotionRequest;
import org.gomsu.productservice.dto.response.PromotionResponse;
import org.gomsu.productservice.entity.Product;
import org.gomsu.productservice.entity.ProductPromotion;
import org.gomsu.productservice.entity.Promotion;
import org.gomsu.productservice.repository.CategoryRepository;
import org.gomsu.productservice.repository.ProductRepository;
import org.gomsu.productservice.repository.PromotionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {
    private final PromotionRepository promotionRepository;

    private final ProductRepository productRepository; // Cần cái này để tìm Product từ ID

    //Tao promotion moi
    @Transactional
    public PromotionResponse createPromotion(PromotionRequest promotionRequest) {
        Promotion promotion = new Promotion();
        promotion.setName(promotionRequest.getName());
        promotion.setStartDate(promotionRequest.getStartDate());
        promotion.setEndDate(promotionRequest.getEndDate());
        List<ProductPromotion> productPromotions = promotionRequest.getProductDiscounts().stream()
                .map(item -> {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
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
    public PromotionResponse updatePromotion(Long id, PromotionRequest request) {
        // 1. Kiểm tra sự tồn tại của chương trình khuyến mãi trong DB
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi ID: " + id));

        // 2. Cập nhật các thông tin cơ bản của chương trình
        promotion.setName(request.getName());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());

        // 3. Tạo một bản đồ (Map) chứa các sản phẩm khuyến mãi hiện đang có trong DB
        // Key: productId, Value: đối tượng ProductPromotion
        // Mục đích: Để kiểm tra nhanh xem sản phẩm trong request đã tồn tại chưa
        Map<Long, ProductPromotion> existingMap = promotion.getProductPromotions().stream()
                .collect(Collectors.toMap(pp -> pp.getProduct().getId(), pp -> pp));

        // 4. Xử lý danh sách sản phẩm giảm giá gửi lên từ Request
        List<ProductPromotion> updatedList = request.getProductDiscounts().stream()
                .map(dto -> {
                    // TRƯỜNG HỢP 1: Sản phẩm đã có trong chương trình khuyến mãi này rồi
                    if (existingMap.containsKey(dto.getProductId())) {
                        ProductPromotion existing = existingMap.get(dto.getProductId());
                        // Cập nhật lại phần trăm giảm giá mới
                        existing.setDiscountPercentage(dto.getDiscountPercentage());
                        // Xóa khỏi Map để đánh dấu là sản phẩm này vẫn được giữ lại
                        existingMap.remove(dto.getProductId());
                        return existing;
                    }
                    // TRƯỜNG HỢP 2: Sản phẩm mới được thêm vào chương trình
                    else {
                        Product product = productRepository.findById(dto.getProductId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + dto.getProductId()));

                        ProductPromotion pp = new ProductPromotion();
                        pp.setProduct(product);
                        pp.setPromotion(promotion); // Thiết lập mối quan hệ với bảng Promotion (Cha)
                        pp.setDiscountPercentage(dto.getDiscountPercentage());
                        return pp;
                    }
                }).collect(Collectors.toList());

        // 5. Đồng bộ hóa danh sách (Sync List)
        // Xóa sạch danh sách cũ trong bộ nhớ (nhờ orphanRemoval=true nên những thằng bị xóa ở bước 4 sẽ mất khỏi DB)
        promotion.getProductPromotions().clear();
        // Thêm danh sách đã được cập nhật/thêm mới vào lại
        promotion.getProductPromotions().addAll(updatedList);

        // 6. Lưu lại vào Database và chuyển đổi sang đối tượng Response để trả về cho Client
        return toPromotionResponse(promotionRepository.save(promotion));
    }

    // Dừng khuyến mãi ngay lập tức (Kết thúc sớm)
    @Transactional
    public PromotionResponse stopPromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt khuyến mãi này"));

        // Gán ngày kết thúc là bây giờ
        promotion.setEndDate(java.time.LocalDateTime.now());

        return toPromotionResponse(promotionRepository.save(promotion));
    }

    // Hàm xóa Promotion
    @Transactional
    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt khuyến mãi này"));

        // Clear danh sách để Hibernate hiểu là cần xóa các bản ghi con trước
        promotion.getProductPromotions().clear();
        promotionRepository.delete(promotion);
    }

    // Lấy chi tiết một đợt khuyến mãi
    public PromotionResponse getPromotionById(Long id) {
        // Tìm Promotion trong DB, nếu không có thì ném lỗi
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt khuyến mãi với ID: " + id));

        // Dùng chính cái hàm "phù thủy" toPromotionResponse để biến nó thành DTO đẹp đẽ
        return toPromotionResponse(promotion);
    }

    // Lấy tất cả đợt khuyến mãi (Phân trang)
    public Page<PromotionResponse> getAllPromotions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
        return promotionRepository.findAll(pageable)
                .map(this::toPromotionResponse);
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
                            .discountPercentage(discountPercentage)
                            .originalPrice(originalPrice)
                            .discountedPrice(discountedPrice)
                            .build();
                })
                .collect(Collectors.toList());
        return PromotionResponse.builder()
                .id(promotion.getId())
                .name(promotion.getName())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .items(items)
                .build();
    }

}
