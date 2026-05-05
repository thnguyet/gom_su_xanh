package org.gomsu.productservice.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gomsu.productservice.entity.Category;
import org.gomsu.productservice.entity.Product;
import org.gomsu.productservice.entity.ProductImage;
import org.gomsu.productservice.repository.CategoryRepository;
import org.gomsu.productservice.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info(">>> Database đã có dữ liệu sản phẩm, bỏ qua seeding.");
            return;
        }

        log.info(">>> Bắt đầu seeding dữ liệu sản phẩm mẫu...");

        // ===== TẠO DANH MỤC =====
        Category catTranh = categoryRepository.save(Category.builder()
                .name("Tranh gốm tái chế").slug("tranh-gom-tai-che").build());
        Category catMocKhoa = categoryRepository.save(Category.builder()
                .name("Móc khóa").slug("moc-khoa").build());
        Category catFridge = categoryRepository.save(Category.builder()
                .name("Fridge magnet").slug("fridge-magnet").build());
        Category catBoKit = categoryRepository.save(Category.builder()
                .name("Bộ kit thủ công").slug("bo-kit-thu-cong").build());
        Category catKintsugi = categoryRepository.save(Category.builder()
                .name("Sản phẩm Kintsugi").slug("san-pham-kintsugi").build());

        log.info(">>> Đã tạo {} danh mục.", 5);

        // ===== TẠO SẢN PHẨM NỔI BẬT (từ trang chủ) =====

        // 1. Bộ kit vẽ tranh gốm sứ
        Product p1 = productRepository.save(Product.builder()
                .name("Bộ kit vẽ tranh gốm sứ")
                .slug("bo-kit-ve-tranh-gom-su")
                .brand("Gốm Sứ Xanh")
                .price(60000.0)
                .description("Bộ kit vẽ tranh gốm sứ tái chế từ phế liệu Bát Tràng. Bao gồm mảnh gốm, màu acrylic, cọ vẽ và khung tranh. Phù hợp cho mọi lứa tuổi, không cần kinh nghiệm.")
                .stockQuantity(50)
                .averageRating(4.8)
                .reviewCount(12L)
                .category(catBoKit)
                .build());
        addImage(p1, "../assets/images/0b4db3fc90f2e86ecc2dabc1484c213072054b4c.jpg");

        // 2. Tranh Mèo đa sắc
        Product p2 = productRepository.save(Product.builder()
                .name("Tranh Mèo đa sắc")
                .slug("tranh-meo-da-sac")
                .brand("Gốm Sứ Xanh")
                .price(100000.0)
                .description("Tranh gốm tái chế hình mèo đa sắc, được ghép từ hàng trăm mảnh gốm phế liệu Bát Tràng. Mỗi bức tranh là một tác phẩm nghệ thuật độc nhất, mang thông điệp tái chế và bảo vệ môi trường.")
                .stockQuantity(30)
                .averageRating(4.9)
                .reviewCount(8L)
                .category(catTranh)
                .build());
        addImage(p2, "../assets/images/e9f274abe82185f29f7f9e9d201211645b0ebfc1.jpg");

        // 3. Móc khóa Bít tất
        Product p3 = productRepository.save(Product.builder()
                .name("Móc khóa Bít tất")
                .slug("moc-khoa-bit-tat")
                .brand("Gốm Sứ Xanh")
                .price(40000.0)
                .description("Móc khóa gốm sứ tái chế hình bít tất dễ thương. Được làm thủ công từ gốm phế liệu, tô vẽ bằng tay với nhiều màu sắc sinh động. Quà tặng ý nghĩa cho bạn bè và người thân.")
                .stockQuantity(100)
                .averageRating(4.7)
                .reviewCount(15L)
                .category(catMocKhoa)
                .build());
        addImage(p3, "../assets/images/acf1adfc9c44d707e39fade2f0936e22060b17da.jpg");

        // ===== SẢN PHẨM BỔ SUNG (từ các trang danh mục) =====

        // 4. Fridge magnet Hoa Sen
        Product p4 = productRepository.save(Product.builder()
                .name("Fridge magnet Hoa Sen")
                .slug("fridge-magnet-hoa-sen")
                .brand("Gốm Sứ Xanh")
                .price(35000.0)
                .description("Nam châm tủ lạnh hình hoa sen, được làm từ mảnh gốm tái chế Bát Tràng. Trang trí tủ lạnh thêm sinh động với hoa văn truyền thống Việt Nam.")
                .stockQuantity(80)
                .averageRating(4.6)
                .reviewCount(10L)
                .category(catFridge)
                .build());
        addImage(p4, "../assets/images/3838a9d43772b2011d5823350fb83965460ef7e7.jpg");

        // 5. Bộ kit thủ công Mèo
        Product p5 = productRepository.save(Product.builder()
                .name("Bộ kit thủ công Mèo")
                .slug("bo-kit-thu-cong-meo")
                .brand("Gốm Sứ Xanh")
                .price(55000.0)
                .description("Bộ kit thủ công tạo hình mèo từ gốm tái chế. Bao gồm khuôn gốm, màu vẽ, cọ và hướng dẫn chi tiết. Hoạt động sáng tạo thú vị cho cả gia đình.")
                .stockQuantity(40)
                .averageRating(4.5)
                .reviewCount(6L)
                .category(catBoKit)
                .build());
        addImage(p5, "../assets/images/be2e3a2e3c83489a5a3914bd568b06c5c09930d2.jpg");

        // 6. Bình Bạch Kim Kintsugi
        Product p6 = productRepository.save(Product.builder()
                .name("Bình Bạch Kim Kintsugi")
                .slug("binh-bach-kim-kintsugi")
                .brand("Gốm Sứ Xanh")
                .price(250000.0)
                .description("Bình gốm Kintsugi phong cách Nhật Bản, tôn vinh vẻ đẹp của sự không hoàn hảo. Các đường nứt được hàn gắn bằng sơn vàng kim, tạo nên tác phẩm nghệ thuật độc đáo.")
                .stockQuantity(15)
                .averageRating(5.0)
                .reviewCount(4L)
                .category(catKintsugi)
                .build());
        addImage(p6, "../assets/images/95624edfa4edfccd25b11e43ac08e222708024d5.jpg");

        // 7. Móc khóa Ngôi sao
        Product p7 = productRepository.save(Product.builder()
                .name("Móc khóa Ngôi sao")
                .slug("moc-khoa-ngoi-sao")
                .brand("Gốm Sứ Xanh")
                .price(35000.0)
                .description("Móc khóa gốm sứ tái chế hình ngôi sao, được tô vẽ thủ công với nhiều màu sắc tươi sáng. Món quà nhỏ mang ý nghĩa lớn về bảo vệ môi trường.")
                .stockQuantity(90)
                .averageRating(4.6)
                .reviewCount(9L)
                .category(catMocKhoa)
                .build());
        addImage(p7, "../assets/images/4f104504f6e60a16bf34ab9632875014e953e479.jpg");

        // 8. Tranh gốm Phong cảnh Bát Tràng
        Product p8 = productRepository.save(Product.builder()
                .name("Tranh gốm Phong cảnh Bát Tràng")
                .slug("tranh-gom-phong-canh-bat-trang")
                .brand("Gốm Sứ Xanh")
                .price(150000.0)
                .description("Tranh ghép gốm tái chế mô tả phong cảnh làng gốm Bát Tràng. Sử dụng các mảnh gốm nhiều màu sắc khác nhau để tái hiện vẻ đẹp truyền thống của làng nghề.")
                .stockQuantity(20)
                .averageRating(4.8)
                .reviewCount(7L)
                .category(catTranh)
                .build());
        addImage(p8, "../assets/images/5671cbed570674bbb8b7a8070ed765851887e396.jpg");

        log.info(">>> Seeding hoàn tất! Đã thêm {} sản phẩm vào database.", 8);
    }

    private void addImage(Product product, String imageUrl) {
        ProductImage image = new ProductImage();
        image.setImageUrl(imageUrl);
        image.setProduct(product);
        if (product.getProductImages() == null) {
            product.setProductImages(new ArrayList<>());
        }
        product.getProductImages().add(image);
        productRepository.save(product);
    }
}
