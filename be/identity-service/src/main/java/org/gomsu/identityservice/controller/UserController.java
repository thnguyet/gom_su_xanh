package org.gomsu.identityservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gomsu.identityservice.dto.request.PasswordChangeRequest;
import org.gomsu.identityservice.dto.request.UserCreationRequest;
import org.gomsu.identityservice.dto.request.UserUpdateRequest;
import org.gomsu.identityservice.dto.response.UserResponse;
import org.gomsu.identityservice.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final org.gomsu.identityservice.service.OtpService otpService;

    @PostMapping("/send-otp")
    public String sendOtp(@RequestParam String email, @RequestParam(required = false) String type) {
        otpService.generateAndSendOtp(email, type);
        return "Mã OTP đã được gửi đến email của bạn!";
    }

    @PostMapping("/register")
    public UserResponse createUser(@RequestBody @Valid UserCreationRequest userCreationRequest) {
        return userService.createUser(userCreationRequest);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getUsers(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String role) {
        return userService.getAllUsers(keyword, role);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUserById(@PathVariable Long userId) {
        userService.deleteUserById(userId);
        return "Tài khoản đã bị xóa!";
    }

    @GetMapping("/my-infor")
    public UserResponse getMyInfor() {
        return userService.getMyInfor();
    }

    @PutMapping("/my-infor")
    public UserResponse updateMyInfor(@Valid @RequestBody UserUpdateRequest userUpdateRequest) {
        return userService.updateMyInfor(userUpdateRequest);
    }

    @PutMapping("/changePassword")
    public UserResponse changePassword(@RequestBody @Valid PasswordChangeRequest passwordChangeRequest) {
        return userService.changePassword(passwordChangeRequest);
    }

    @PutMapping("/changeEmail")
    public org.gomsu.identityservice.dto.response.AuthenticationResponse changeEmail(@RequestBody @Valid org.gomsu.identityservice.dto.request.EmailChangeRequest emailChangeRequest) {
        return userService.changeEmail(emailChangeRequest);
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestBody @Valid org.gomsu.identityservice.dto.request.ResetPasswordRequest request) {
        userService.resetPassword(request);
        return "Đặt lại mật khẩu thành công!";
    }

}
