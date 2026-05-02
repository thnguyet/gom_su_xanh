package org.gomsu.identityservice;

import org.gomsu.identityservice.entity.Role;
import org.gomsu.identityservice.entity.RoleName;
import org.gomsu.identityservice.entity.User;
import org.gomsu.identityservice.repository.RoleRepository;
import org.gomsu.identityservice.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
@EnableDiscoveryClient
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner initData(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Lấy hoặc tạo Role ADMIN
            Role adminRole = roleRepository.findByRoleName(RoleName.ADMIN)
                    .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ADMIN).build()));
            
            // Đảm bảo Role USER cũng tồn tại
            if (roleRepository.findByRoleName(RoleName.USER).isEmpty()) {
                roleRepository.save(Role.builder().roleName(RoleName.USER).build());
            }

            // 2. Tạo tài khoản Admin mặc định nếu chưa có
            String adminEmail = "admin@gomsu.vn";
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                Set<Role> roles = new HashSet<>();
                roles.add(adminRole);

                User admin = User.builder()
                        .username("admin")
                        .email(adminEmail)
                        .password(passwordEncoder.encode("Admin@123"))
                        .phone("0000000000") 
                        .address("Hệ thống")
                        .gender("MALE")
                        .roles(roles)
                        .build();
                
                userRepository.save(admin);
                System.out.println(">>> Đã tạo tài khoản Admin thành công: " + adminEmail + " / Admin@123");
            } else {
                System.out.println(">>> Tài khoản Admin đã tồn tại.");
            }
        };
    }
}
