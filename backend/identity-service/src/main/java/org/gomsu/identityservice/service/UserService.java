package org.gomsu.identityservice.service;

import lombok.RequiredArgsConstructor;
import org.gomsu.identityservice.dto.request.PasswordChangeRequest;
import org.gomsu.identityservice.dto.request.UserCreationRequest;
import org.gomsu.identityservice.dto.request.UserUpdateRequest;
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

    public UserResponse createUser(UserCreationRequest userCreationRequest) {
        if (userRepository.existsByUsername(userCreationRequest.getUsername())) {
            throw new RuntimeException("Tên tài khoản đã tồn tại!");
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
        return toUserResponse(userRepository.save(user));
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
        // Lay ten nguoi dung dang dang nhap
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        //Tim xem co khong?
        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));
        return toUserResponse(user);
    }

    public UserResponse updateMyInfor(UserUpdateRequest userUpdateRequest) {
        //Lay ten nguoi dung dang dang nhap
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        //Tim xem co khong?
        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        //Update lai user do
        if (userUpdateRequest.getPhone() != null) {
            user.setPhone(userUpdateRequest.getPhone());
        }
        if (userUpdateRequest.getAddress() != null) {
            user.setAddress(userUpdateRequest.getAddress());
        }
        if (userUpdateRequest.getEmail() != null) {
            user.setEmail(userUpdateRequest.getEmail());
        }
        if (userUpdateRequest.getGender() != null) {
            user.setGender(userUpdateRequest.getGender());
        }
        userRepository.save(user);
        return toUserResponse(user);
    }

    public UserResponse changePassword(PasswordChangeRequest passwordChangeRequest) {
        //Lay ten nguoi dung dang dang nhap
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        //Tim xem co khong?
        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        if (!passwordEncoder.matches(passwordChangeRequest.getOldPassword(), user.getPassword())){
            throw new RuntimeException("Mật khẩu không đúng!");
        }

        user.setPassword(passwordEncoder.encode(passwordChangeRequest.getNewPassword()));
        userRepository.save(user);
        return toUserResponse(user);
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
