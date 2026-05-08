package org.gomsu.identityservice.service;

import lombok.RequiredArgsConstructor;
import org.gomsu.identityservice.dto.request.EmailChangeRequest;
import org.gomsu.identityservice.dto.request.PasswordChangeRequest;
import org.gomsu.identityservice.dto.request.UserCreationRequest;
import org.gomsu.identityservice.dto.request.UserUpdateRequest;
import org.gomsu.identityservice.dto.response.AuthenticationResponse;
import org.gomsu.identityservice.dto.response.UserResponse;
import org.gomsu.identityservice.entity.RoleName;
import org.gomsu.identityservice.entity.User;
import org.gomsu.identityservice.repository.RoleRepository;
import org.gomsu.identityservice.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final AuthenticationService authenticationService;

    public UserResponse createUser(UserCreationRequest userCreationRequest) {
        // Verify OTP
        if (!otpService.verifyOtp(userCreationRequest.getEmail(), userCreationRequest.getOtp())) {
            throw new RuntimeException("Mã OTP không đúng hoặc đã hết hạn!");
        }

        if (userRepository.existsByEmail(userCreationRequest.getEmail())) {
            throw new RuntimeException("Email này đã được đăng ký!");
        }
        if (userRepository.existsByPhone(userCreationRequest.getPhone())) {
            throw new RuntimeException("Số điện thoại này đã được đăng ký!");
        }

        User user = new User();
        user.setUsername(userCreationRequest.getUsername());
        user.setPhone(userCreationRequest.getPhone());
        user.setAddress(userCreationRequest.getAddress());
        user.setEmail(userCreationRequest.getEmail());
        user.setGender(userCreationRequest.getGender());
        user.setPassword(passwordEncoder.encode(userCreationRequest.getPassword()));
        var roleUser = roleRepository.findByRoleName(RoleName.USER)
                        .orElseGet(() -> {
                            org.gomsu.identityservice.entity.Role newRole = new org.gomsu.identityservice.entity.Role().builder()
                                    .roleName(RoleName.USER)
                                    .build();
                            return roleRepository.save(newRole);
                        });
        user.setRoles(Set.of(roleUser));
        UserResponse response = toUserResponse(userRepository.save(user));
        
        // Success, delete OTP
        otpService.deleteOtp(userCreationRequest.getEmail());
        
        return response;
    }

    public List<UserResponse> getAllUsers(String keyword, String role) {
        List<User> users;
        RoleName roleName = null;
        if (role != null && !role.isBlank()) {
            try { roleName = RoleName.valueOf(role.toUpperCase()); } catch (Exception e) {}
        }
        if ((keyword != null && !keyword.isBlank()) || roleName != null) {
            users = userRepository.searchUsers(keyword, roleName);
        } else {
            users = userRepository.findAll();
        }
        return users.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));
        return toUserResponse(user);
    }

    public void deleteUserById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User không tồn tại!");
        }
        userRepository.deleteById(id);
    }

    public UserResponse getMyInfor() {
        // Lay định danh (Email) người dùng đang đăng nhập từ token
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        // Tìm xem có không bằng Email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));
        return toUserResponse(user);
    }

    public UserResponse updateMyInfor(UserUpdateRequest userUpdateRequest) {
        // Lay định danh (Email) người dùng đang đăng nhập từ token
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        // Tìm xem có không bằng Email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        //Update lai user do
        if (userUpdateRequest.getUsername() != null) {
            user.setUsername(userUpdateRequest.getUsername());
        }
        if (userUpdateRequest.getPhone() != null) {
            user.setPhone(userUpdateRequest.getPhone());
        }
        if (userUpdateRequest.getAddress() != null) {
            user.setAddress(userUpdateRequest.getAddress());
        }
        if (userUpdateRequest.getGender() != null) {
            user.setGender(userUpdateRequest.getGender());
        }
        userRepository.save(user);
        return toUserResponse(user);
    }

    public AuthenticationResponse changeEmail(EmailChangeRequest request) {
        var context = SecurityContextHolder.getContext();
        String currentEmail = context.getAuthentication().getName();

        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw new RuntimeException("Email mới đã được đăng ký bởi tài khoản khác!");
        }

        // Verify OTP (Check against the NEW email)
        if (!otpService.verifyOtp(request.getNewEmail(), request.getOtp())) {
            throw new RuntimeException("Mã OTP không đúng hoặc đã hết hạn!");
        }

        user.setEmail(request.getNewEmail());
        userRepository.save(user);

        // Success, delete OTP
        otpService.deleteOtp(request.getNewEmail());

        // Generate NEW token for the new email so user doesn't have to logout
        var expiryTime = new java.util.Date(java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.HOURS).toEpochMilli());
        String newToken = authenticationService.generateToken(user, expiryTime);

        return AuthenticationResponse.builder()
                .token(newToken)
                .authenticated(true)
                .build();
    }

    public UserResponse changePassword(PasswordChangeRequest passwordChangeRequest) {
        // Lay định danh (Email) người dùng đang đăng nhập từ token
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        // Tìm xem có không bằng Email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        if (!passwordEncoder.matches(passwordChangeRequest.getOldPassword(), user.getPassword())){
            throw new RuntimeException("Mật khẩu không đúng!");
        }

        if (passwordEncoder.matches(passwordChangeRequest.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu mới không được trùng với mật khẩu hiện tại!");
        }

        user.setPassword(passwordEncoder.encode(passwordChangeRequest.getNewPassword()));
        userRepository.save(user);
        return toUserResponse(user);
    }

    public void resetPassword(org.gomsu.identityservice.dto.request.ResetPasswordRequest request) {
        // Kiểm tra email tồn tại
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống!"));

        // Xác minh OTP
        if (!otpService.verifyOtp(request.getEmail(), request.getOtp())) {
            throw new RuntimeException("Mã OTP không đúng hoặc đã hết hạn!");
        }

        // Đặt mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Xóa OTP đã dùng
        otpService.deleteOtp(request.getEmail());
    }

    //chuyen entity -> response
    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .address(user.getAddress())
                .email(user.getEmail())
                .gender(user.getGender())
                .roles(user.getRoles().stream()
                        .findFirst() // lay quyen dau tien
                        .map(role -> role.getRoleName().name()) // lay ten role
                        .orElse("USER")) // neu khong co thi dat mac dinh la USER
                .build();
    }

}
