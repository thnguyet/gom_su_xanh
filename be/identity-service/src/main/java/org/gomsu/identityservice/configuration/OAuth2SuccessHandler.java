package org.gomsu.identityservice.configuration;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gomsu.identityservice.entity.Role;
import org.gomsu.identityservice.entity.RoleName;
import org.gomsu.identityservice.entity.User;
import org.gomsu.identityservice.exception.AppException;
import org.gomsu.identityservice.exception.ErrorCode;
import org.gomsu.identityservice.repository.RoleRepository;
import org.gomsu.identityservice.repository.UserRepository;
import org.gomsu.identityservice.service.AuthenticationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthenticationService authenticationService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

            // Log tất cả các thuộc tính nhận được từ OAuth2 để debug
            Map<String, Object> attributes = oauth2User.getAttributes();
            log.info("OAuth2 attributes received: {}", attributes);

            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");

            if (email == null || email.isBlank()) {
                throw new AppException(ErrorCode.OAUTH2_EMAIL_NOT_FOUND);
            }

            log.info("OAuth2 login - Email: {}, Name: {}", email, name);

            // Tìm user trong DB
            User user = userRepository.findByEmail(email).orElse(null);
            boolean isNewUser = (user == null);

            if (isNewUser) {
                log.info("Creating new user from OAuth2: {}", email);

                // Tìm hoặc tạo Role USER
                Role roleUser = roleRepository.findByRoleName(RoleName.USER).orElse(null);
                if (roleUser == null) {
                    log.info("Role USER not found, creating...");
                    roleUser = roleRepository.save(Role.builder().roleName(RoleName.USER).build());
                }

                // Lấy giới tính từ Google (nếu có)
                String genderAttr = oauth2User.getAttribute("gender");
                String displayGender = null;
                if ("male".equalsIgnoreCase(genderAttr)) displayGender = "Nam";
                else if ("female".equalsIgnoreCase(genderAttr)) displayGender = "Nữ";

                // Tạo user mới - KHÔNG cần password, phone, address (DB đã cho phép NULL)
                user = new User();
                user.setEmail(email);
                user.setUsername(name != null ? name : email.split("@")[0]);
                user.setGender(displayGender);
                user.setRoles(Set.of(roleUser));
                // phone, address, password = null (OK vì DB đã cho phép)

                user = userRepository.saveAndFlush(user);
                log.info("New user saved with ID: {}", user.getId());
            } else {
                log.info("Existing user found: {} (ID: {})", email, user.getId());
            }

            // Tạo JWT token
            Date expiryTime = new Date(Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli());
            String token = authenticationService.generateToken(user, expiryTime);
            log.info("JWT token generated successfully");

            // Kiểm tra xem user cần cập nhật thông tin không
            boolean needsUpdate = (user.getPhone() == null || user.getPhone().isBlank()
                    || user.getAddress() == null || user.getAddress().isBlank());

            // Redirect về frontend
            String redirectUrl = "http://localhost:3000/fe-user/pages/trang-chu_417-354.html?token=" + token;
            if (needsUpdate) {
                redirectUrl += "&needsUpdate=true";
            }
            log.info("Redirecting to: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 login failed: {}", e.getMessage(), e);
            response.sendRedirect("http://localhost:3000/fe-user/pages/dang-nhap_839-134.html?error=oauth2_failure");
        }
    }
}
