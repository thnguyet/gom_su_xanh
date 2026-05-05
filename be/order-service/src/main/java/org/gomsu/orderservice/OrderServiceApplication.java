package org.gomsu.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import org.gomsu.orderservice.entity.PaymentMethod;
import org.gomsu.orderservice.entity.ShippingMethod;
import org.gomsu.orderservice.repository.PaymentMethodRepository;
import org.gomsu.orderservice.repository.ShippingMethodRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing
@EnableDiscoveryClient
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner initData(ShippingMethodRepository shipRepo, PaymentMethodRepository payRepo) {
        return args -> {
            if (shipRepo.count() == 0) {
                System.out.println(">>> Đang khởi tạo đơn vị vận chuyển...");
                ShippingMethod sn = new ShippingMethod();
                sn.setName("Giao hàng Nhanh");
                sn.setShippingFee(30000.0);
                sn.setActive(true);
                shipRepo.save(sn);

                ShippingMethod st = new ShippingMethod();
                st.setName("Giao hàng Tiết kiệm");
                st.setShippingFee(15000.0);
                st.setActive(true);
                shipRepo.save(st);
            }
            if (payRepo.count() == 0) {
                System.out.println(">>> Đang khởi tạo phương thức thanh toán...");
                PaymentMethod p1 = new PaymentMethod();
                p1.setName("Chuyển khoản ngân hàng");
                p1.setPaymentCode("BANK_TRANSFER");
                p1.setActive(true);
                payRepo.save(p1);

                PaymentMethod p2 = new PaymentMethod();
                p2.setName("Thanh toán khi nhận hàng (COD)");
                p2.setPaymentCode("COD");
                p2.setActive(true);
                payRepo.save(p2);
            }
        };
    }

}
