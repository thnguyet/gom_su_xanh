package org.gomsu.productservice.service;

import lombok.RequiredArgsConstructor;
import org.gomsu.productservice.dto.request.ProductCreationRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final CloudinaryService cloudinaryService;

    //Them san pham
    public ProductResponse createProduct(ProductCreationRequest productCreationRequest, List<MultipartFile> images) {
        Product product = new Product();
        product.setName(productCreationRequest.getName());
        product.setPrice(productCreationRequest.getPrice());
        product.setDescription(productCreationRequest.getDescription());
        product.setBrand(productCreationRequest.getBrand());
        product.setStockQuantity(productCreationRequest.getStockQuantity());

        Category category = categoryRepository.findById(productCreationRequest.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID này!"));
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);

        // 3. XỬ LÝ UPLOAD DANH SÁCH ẢNH
        List<ProductImage> savedImages = new ArrayList<>();

        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                try {
                    // Đẩy lên mây lấy link
                    String imageUrl = cloudinaryService.uploadImage(file);

                    // Tạo đối tượng ProductImage và lưu DB
                    ProductImage productImage = new ProductImage();
                    productImage.setImageUrl(imageUrl);
                    productImage.setProduct(savedProduct); // Gắn với áo vừa tạo

                    savedImages.add(productImageRepository.save(productImage));
                } catch (IOException e) {
                    throw new RuntimeException("Lỗi khi upload ảnh: " + e.getMessage());
                }
            }
        }

        savedProduct.setProductImages(savedImages);

        return toProductResponse(savedProduct);
    }

    //Sua san pham
    public ProductResponse updateProduct(ProductUpdateRequest productUpdateRequest, Long id, List<MultipartFile> images) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm!"));

        if (productUpdateRequest.getName() != null && !productUpdateRequest.getName().isEmpty()) {
            product.setName(productUpdateRequest.getName());
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

        // Them anh
        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                try {
                    String imageUrl = cloudinaryService.uploadImage(file);
                    ProductImage productImage = new ProductImage();
                    productImage.setImageUrl(imageUrl);
                    productImage.setProduct(product);
                    product.getProductImages().add(productImageRepository.save(productImage));
                } catch (IOException e) {
                    throw new RuntimeException("Lỗi khi upload ảnh mới");
                }
            }
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

    //Xoa san pham
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
    public Page<ProductResponse> getAllProducts(int page, int size, String keyword, String sortBy, String sortDir) {
        // 1. Quyết định chiều sắp xếp (Tăng dần - ASC hay Giảm dần - DESC)
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // 2. Tạo đối tượng Pageable bao gồm: Trang số mấy + Kích thước + Sắp xếp thế nào
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> productPage;

        if (keyword != null && !keyword.isEmpty()) {
            productPage = productRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        }
        else {
            productPage = productRepository.findAll(pageable);
        }

        return productPage.map(this::toProductResponse);
    }

    //Xem 1 san pham theo id
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm này"));
        return this.toProductResponse(product);
    }

    //Them anh cho san pham
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

    private ProductResponse toProductResponse(Product product)
    {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .brand(product.getBrand())
                .stockQuantity(product.getStockQuantity())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .imageUrls(product.getProductImages() != null ?
                        product.getProductImages().stream()
                            .map(ProductImage::getImageUrl)
                            .toList() : null)
                .discountPercentage(calculateBestDiscount(product))
                .build();
    }
}
