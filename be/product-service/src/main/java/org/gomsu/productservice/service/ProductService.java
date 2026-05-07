package org.gomsu.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gomsu.productservice.dto.request.ProductCreationRequest;
import org.gomsu.productservice.dto.request.ProductRestockRequest;
import org.gomsu.productservice.dto.request.ProductUpdateRequest;
import org.gomsu.productservice.dto.response.ProductResponse;
import org.gomsu.productservice.entity.*;
import org.gomsu.productservice.repository.CategoryRepository;
import org.gomsu.productservice.repository.ProductImageRepository;
import org.gomsu.productservice.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final CloudinaryService cloudinaryService;

    //Them san pham
    @Transactional
    public ProductResponse createProduct(ProductCreationRequest productCreationRequest, List<MultipartFile> images) {
        Product product = Product.builder()
                .name(productCreationRequest.getName())
                .slug(toSlug(productCreationRequest.getName())) // Tự động tạo slug từ tên
                .price(productCreationRequest.getPrice())
                .description(productCreationRequest.getDescription())
                .brand(productCreationRequest.getBrand())
                .stockQuantity(productCreationRequest.getStockQuantity())
                .build();

        Category category = categoryRepository.findById(productCreationRequest.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID này!"));
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);

        // 3. XỬ LÝ UPLOAD DANH SÁCH ẢNH
//        List<ProductImage> savedImages = new ArrayList<>();
//
//        if (images != null && !images.isEmpty()) {
//            for (MultipartFile file : images) {
//                try {
//                    // Đẩy lên mây lấy link
//                    String imageUrl = cloudinaryService.uploadImage(file);
//
//                    // Tạo đối tượng ProductImage và lưu DB
//                    ProductImage productImage = new ProductImage();
//                    productImage.setImageUrl(imageUrl);
//                    productImage.setProduct(savedProduct); // Gắn với áo vừa tạo
//
//                    savedImages.add(productImageRepository.save(productImage));
//                } catch (IOException e) {
//                    throw new RuntimeException("Lỗi khi upload ảnh: " + e.getMessage());
//                }
//            }
//        }
        // Xử lý upload ảnh (Dùng hàm dùng chung để code ngắn gọn hơn)
        if (images != null && !images.isEmpty()) {
            handleImageUploads(images, savedProduct);
        }

        // Xử lý thêm link ảnh từ Request (nếu có)
        if (productCreationRequest.getImageUrls() != null && !productCreationRequest.getImageUrls().isEmpty()) {
            for (String url : productCreationRequest.getImageUrls()) {
                if (url == null || url.isBlank()) continue;
                ProductImage productImage = new ProductImage();
                productImage.setImageUrl(url);
                productImage.setProduct(savedProduct);
                productImageRepository.save(productImage);
                if (savedProduct.getProductImages() == null) savedProduct.setProductImages(new ArrayList<>());
                savedProduct.getProductImages().add(productImage);
            }
        }

        return toProductResponse(savedProduct);
    }

    // --- HÀM HỖ TRỢ UPLOAD ẢNH (Tách ra để dùng chung) ---
    private void handleImageUploads(List<MultipartFile> images, Product product) {
        for (MultipartFile file : images) {
            try {
                String imageUrl = cloudinaryService.uploadImage(file);
                ProductImage productImage = new ProductImage();
                productImage.setImageUrl(imageUrl);
                productImage.setProduct(product);
                productImageRepository.save(productImage);
                // Thêm vào list của product để toProductResponse lấy được ngay
                if (product.getProductImages() == null) product.setProductImages(new ArrayList<>());
                product.getProductImages().add(productImage);
            } catch (IOException e) {
                throw new RuntimeException("Lỗi upload ảnh");
            }
        }
    }

    //Sua san pham
    @Transactional
    public ProductResponse updateProduct(ProductUpdateRequest productUpdateRequest, Long id, List<MultipartFile> images) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm!"));

        if (productUpdateRequest.getName() != null && !productUpdateRequest.getName().isEmpty()) {
            product.setName(productUpdateRequest.getName());
            product.setSlug(toSlug(productUpdateRequest.getName()));
        }

        if (productUpdateRequest.getPrice() != null && productUpdateRequest.getPrice() != 0) {
            product.setPrice(productUpdateRequest.getPrice());
        }

        if (productUpdateRequest.getDescription() != null && !productUpdateRequest.getDescription().isEmpty()) {
            product.setDescription(productUpdateRequest.getDescription());
        }

        if (productUpdateRequest.getBrand() != null && !productUpdateRequest.getBrand().isEmpty()) {
            product.setBrand(productUpdateRequest.getBrand());
        }

        if (productUpdateRequest.getStockQuantity() != null && productUpdateRequest.getStockQuantity() != 0) {
            product.setStockQuantity(productUpdateRequest.getStockQuantity());
        }

        // Thêm ảnh mới
        if (images != null && !images.isEmpty()) {
            handleImageUploads(images, product);
        }

        //Xoa anh
        if (productUpdateRequest.getDeletedImageIds() != null && !productUpdateRequest.getDeletedImageIds().isEmpty()) {
            product.getProductImages().removeIf(productImage -> {
                if (productUpdateRequest.getDeletedImageIds().contains(productImage.getId())) {
                    try {
                        cloudinaryService.deleteImage(productImage.getImageUrl());
                        return true;
                    } catch (IOException e) {
                        throw new RuntimeException("Lỗi khi xóa ảnh trên Cloudinary");
                    }
                }
                return false;
            });
        }

        return toProductResponse(productRepository.save(product));
    }

    // 3. THÊM HÀM LẤY SẢN PHẨM THEO SLUG (Dành cho khách xem trang chi tiết)
    public ProductResponse getProductBySlug(String slug) {
        Optional<Product> productOpt = productRepository.findBySlug(slug);

        // Nếu không tìm thấy theo slug, thử tìm theo ID nếu slug là số
        if (productOpt.isEmpty() && slug.matches("\\d+")) {
            productOpt = productRepository.findById(Long.parseLong(slug));
        }

        Product product = productOpt.orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));
        return toProductResponse(product);
    }

    //Xoa san pham
    @Transactional
    public void deleteProduct(Long id) {
        Product deleteProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm cần xóa!"));
        if(deleteProduct.getProductImages() != null && !deleteProduct.getProductImages().isEmpty()) {
            for (ProductImage productImage : deleteProduct.getProductImages()) {
                try {
                    cloudinaryService.deleteImage(productImage.getImageUrl());
                } catch (IOException e) {
                    System.err.println("Cảnh báo: Không thể xóa ảnh này trên Cloudinary");
                }
            }
        }
        productRepository.deleteById(id);
    }

    //Xem toan bo san pham
    @Transactional
    public Page<ProductResponse> getAllProducts(
            int page,
            int size,
            String keyword,
            Long categoryId,
            Double minPrice,
            Double maxPrice,
            String sortBy,
            String sortDir,
            Boolean active,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        // 1. Quyết định chiều sắp xếp
        // Nếu muốn mặc định cái mới nhất hiện lên đầu, Nguyệt có thể set sortBy = "createdAt"
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // 2. Sử dụng Specification hoặc Query tùy biến để lọc theo thời gian
        // Ở đây mình hướng dẫn cách dùng đơn giản nhất là bổ sung Query vào Repository
        Page<Product> productPage;

        // Nguyệt cần xử lý logic lọc kết hợp: Keyword + Thời gian
        // Mình giả định Nguyệt sẽ dùng một hàm Search tổng hợp trong Repository
        productPage = productRepository.searchProducts(
                keyword != null ? keyword.trim() : null,
                categoryId,
                minPrice,
                maxPrice,
                fromDate,
                toDate,
                active,
                pageable);

        return productPage.map(this::toProductResponse);
    }

    //Xem 1 san pham theo id
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm này"));
        return this.toProductResponse(product);
    }

    //Them anh cho san pham
    @Transactional
    public ProductResponse uploadProductImages(Long id, List<MultipartFile> images) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm này"));
        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                try {
                    String imgageUrl = cloudinaryService.uploadImage(file);
                    ProductImage productImage = new ProductImage();
                    productImage.setImageUrl(imgageUrl);
                    productImage.setProduct(product);
                    product.getProductImages().add(productImageRepository.save(productImage));
                } catch (IOException e) {
                    throw new RuntimeException("Lỗi khi upload ảnh mới");
                }
            }
            productRepository.save(product);
        }

        return this.toProductResponse(product);
    }

    private Double calculateBestDiscount(Product product) {
        LocalDateTime now = LocalDateTime.now();

        if (product.getProductPromotions() == null) return 0.0;

        return product.getProductPromotions().stream()
                .filter(pp -> {
                    Promotion promo = pp.getPromotion();
                    // ✅ Kiểm tra kỹ: Nếu promo hoặc ngày tháng bị null thì bỏ qua luôn
                    return promo != null
                            && promo.getStartDate() != null
                            && promo.getEndDate() != null
                            && promo.getStartDate().isBefore(now)
                            && promo.getEndDate().isAfter(now);
                })
                .map(pp -> pp.getDiscountPercentage() != null ? pp.getDiscountPercentage() : 0.0)
                .max(Double::compare)
                .orElse(0.0);
    }

    public List<ProductResponse> getProductsByIds(List<Long> ids) {
        List<Product> products = productRepository.findAllById(ids);
        return products.stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public  void restockProducts(List<ProductRestockRequest> restockRequests) {
            for (ProductRestockRequest restockRequest : restockRequests) {
                // Tim san pham trong DB
                Product product = productRepository.findById(restockRequest.getProductId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + restockRequest.getProductId()));

                // Cong lai so luong
                int newQuantity = product.getStockQuantity() + restockRequest.getQuantity();
                product.setStockQuantity(newQuantity);

                productRepository.save(product);
            }
    }

    @Transactional
    public void reduceStock(List<ProductRestockRequest> requests) {
        for (ProductRestockRequest req : requests) {
            Product product = productRepository.findById(req.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + req.getProductId()));

            if (product.getStockQuantity() < req.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + product.getName() + " đã hết hàng hoặc không đủ số lượng!");
            }

            product.setStockQuantity(product.getStockQuantity() - req.getQuantity());
            productRepository.save(product);
        }
    }

    @Transactional
    public void updateProductRating(Long productId, Double averageRating, Long reviewCount) {
        // 1. Tìm sản phẩm
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + productId));

        // 2. Xử lý giá trị NULL trước khi so sánh
        // Nếu trong DB là null thì coi như là 0.0 hoặc 0
        Double currentAvg = (product.getAverageRating() != null) ? product.getAverageRating() : 0.0;
        Long currentCount = (product.getReviewCount() != null) ? product.getReviewCount() : 0L;

        // So sánh an toàn
        if (currentAvg.equals(averageRating) && currentCount.equals(reviewCount)) {
            log.info(">>> Rating SP {} không đổi, bỏ qua cập nhật.", productId);
            return;
        }

        // 3. Cập nhật thông số
        product.setAverageRating(averageRating);
        product.setReviewCount(reviewCount);

        log.info(">>> Cập nhật Rating thành công cho SP {}: {} sao, {} đánh giá",
                productId, averageRating, reviewCount);
    }

    private ProductResponse toProductResponse(Product product) {
        List<ProductResponse.ImageInfo> imagesInfo = (product.getProductImages() != null)
                ? product.getProductImages().stream()
                .map(img -> ProductResponse.ImageInfo.builder()
                        .id(img.getId())
                        .url(img.getImageUrl())
                        .build())
                .toList()
                : new ArrayList<>();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .price(product.getPrice())
                .description(product.getDescription())
                .brand(product.getBrand())
                .stockQuantity(product.getStockQuantity())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .imageUrls(imagesInfo.stream().map(ProductResponse.ImageInfo::getUrl).toList())
                .imagesInfo(imagesInfo)
                .discountPercentage(calculateBestDiscount(product))
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .averageRating(product.getAverageRating() != null ? product.getAverageRating() : 0.0)
                .reviewCount(product.getReviewCount() != null ? product.getReviewCount() : 0)
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
