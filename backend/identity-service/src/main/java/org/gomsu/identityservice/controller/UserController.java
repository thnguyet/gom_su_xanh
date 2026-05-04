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
    public UserResponse updateMyInfor(@RequestBody UserUpdateRequest userUpdateRequest) {
        return userService.updateMyInfor(userUpdateRequest);
    }

    @PutMapping("/changePassword")
    public UserResponse changePassword(@RequestBody PasswordChangeRequest passwordChangeRequest) {
        return userService.changePassword(passwordChangeRequest);
    }

}
