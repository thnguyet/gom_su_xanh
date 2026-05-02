package com.gomsu.workshopservice.configuration;

import com.gomsu.workshopservice.entity.Workshop;
import com.gomsu.workshopservice.entity.WorkshopImage;
import com.gomsu.workshopservice.repository.WorkshopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final WorkshopRepository workshopRepository;

    @Override
    public void run(String... args) {
        if (workshopRepository.count() > 0) {
            log.info(">>> Database đã có dữ liệu workshop, bỏ qua seeding.");
            return;
        }

        log.info(">>> Bắt đầu seeding dữ liệu workshop mẫu...");

        // Thời gian tổ chức: 2 tuần nữa kể từ hôm nay
        LocalDateTime startDate = LocalDateTime.now().plusWeeks(2);
        LocalDateTime endDate = startDate.plusHours(3);

        // Thời gian đăng ký: từ bây giờ đến 1 ngày trước khi workshop diễn ra
        LocalDateTime regStart = LocalDateTime.now();
        LocalDateTime regEnd = startDate.minusDays(1);

        // 1. Vẽ tranh gốm sứ tái chế
        Workshop ws1 = workshopRepository.save(Workshop.builder()
                .name("Vẽ tranh gốm sứ tái chế")
                .slug("ve-tranh-gom-su-tai-che")
                .description("Workshop vẽ tranh từ mảnh gốm sứ tái chế Bát Tràng. Bạn sẽ được hướng dẫn cách chọn mảnh gốm, phối màu và ghép thành bức tranh nghệ thuật độc đáo mang phong cách riêng.")
                .content("Workshop bao gồm:\n- Giới thiệu về nghệ thuật tái chế gốm sứ\n- Hướng dẫn kỹ thuật vẽ trên mảnh gốm\n- Thực hành tạo tác phẩm tranh gốm cá nhân\n- Được mang tác phẩm về nhà")
                .startDate(startDate)
                .endDate(endDate)
                .registrationStartDate(regStart)
                .registrationEndDate(regEnd)
                .price(50000.0)
                .maxParticipants(30)
                .currentParticipants(0)
                .location("Tầng 2, Bảo tàng Gốm Bát Tràng, 28 Đường Bát Tràng, Hà Nội")
                .targetAudience("Mọi lứa tuổi, không cần kinh nghiệm vẽ")
                .tools("Mảnh gốm tái chế, màu acrylic, cọ vẽ, khung tranh (đã bao gồm trong giá)")
                .benefits("Tạo ra tác phẩm nghệ thuật độc đáo, trải nghiệm văn hóa gốm Bát Tràng, góp phần bảo vệ môi trường")
                .active(true)
                .images(new ArrayList<>())
                .build());
        addImage(ws1, "../assets/images/8162b2504c89400a58a317db7e01975fab0734a8.jpg");

        // 2. Vẽ móc chìa khóa
        Workshop ws2 = workshopRepository.save(Workshop.builder()
                .name("Vẽ móc chìa khóa")
                .slug("ve-moc-chia-khoa")
                .description("Workshop tạo móc chìa khóa gốm sứ tái chế. Bạn sẽ tự tay nặn, tạo hình và tô vẽ lên mảnh gốm để tạo ra chiếc móc khóa độc nhất vô nhị.")
                .content("Workshop bao gồm:\n- Hướng dẫn chọn hình dáng và kích thước\n- Tô vẽ và trang trí lên mảnh gốm\n- Gắn phụ kiện móc khóa\n- Được mang sản phẩm về nhà")
                .startDate(startDate.plusDays(1))
                .endDate(endDate.plusDays(1))
                .registrationStartDate(regStart)
                .registrationEndDate(regEnd)
                .price(40000.0)
                .maxParticipants(25)
                .currentParticipants(0)
                .location("Tầng 2, Bảo tàng Gốm Bát Tràng, 28 Đường Bát Tràng, Hà Nội")
                .targetAudience("Trẻ em từ 6 tuổi, thanh thiếu niên, người lớn")
                .tools("Mảnh gốm, màu vẽ, cọ, phụ kiện móc khóa (đã bao gồm trong giá)")
                .benefits("Sáng tạo món quà handmade, rèn luyện khéo tay, thư giãn tinh thần")
                .active(true)
                .images(new ArrayList<>())
                .build());
        addImage(ws2, "../assets/images/d4d90d37e873d1163efcee80bd7f9faf7ad05d12.png");

        // 3. Vẽ fridge magnets
        Workshop ws3 = workshopRepository.save(Workshop.builder()
                .name("Vẽ fridge magnets")
                .slug("ve-fridge-magnets")
                .description("Workshop tạo nam châm tủ lạnh (fridge magnet) từ gốm sứ tái chế. Hoạt động thú vị cho cả gia đình, tạo ra những chiếc magnet xinh xắn trang trí tủ lạnh.")
                .content("Workshop bao gồm:\n- Giới thiệu về fridge magnets gốm tái chế\n- Chọn hình dáng và thiết kế\n- Tô vẽ và trang trí\n- Gắn nam châm và hoàn thiện sản phẩm")
                .startDate(startDate.plusDays(2))
                .endDate(endDate.plusDays(2))
                .registrationStartDate(regStart)
                .registrationEndDate(regEnd)
                .price(40000.0)
                .maxParticipants(35)
                .currentParticipants(0)
                .location("Tầng 2, Bảo tàng Gốm Bát Tràng, 28 Đường Bát Tràng, Hà Nội")
                .targetAudience("Gia đình, nhóm bạn, team building công ty")
                .tools("Mảnh gốm, màu acrylic, cọ, nam châm dán (đã bao gồm trong giá)")
                .benefits("Hoạt động team building vui vẻ, tạo quà lưu niệm, trải nghiệm nghệ thuật tái chế")
                .active(true)
                .images(new ArrayList<>())
                .build());
        addImage(ws3, "../assets/images/a6999bec49da0eed15baef1fe8c4b77c7b636b49.jpg");

        log.info(">>> Seeding hoàn tất! Đã thêm 3 workshop vào database.");
    }

    private void addImage(Workshop workshop, String imageUrl) {
        WorkshopImage image = WorkshopImage.builder()
                .imageUrl(imageUrl)
                .workshop(workshop)
                .build();
        workshop.getImages().add(image);
        workshopRepository.save(workshop);
    }
}
