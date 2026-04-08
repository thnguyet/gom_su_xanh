package org.gomsu.identityservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gomsu.identityservice.dto.request.AuthenticationRequest;
import org.gomsu.identityservice.dto.request.IntrospectRequest;
import org.gomsu.identityservice.dto.request.LogoutRequest;
import org.gomsu.identityservice.dto.request.UserCreationRequest;
import org.gomsu.identityservice.dto.response.AuthenticationResponse;
import org.gomsu.identityservice.dto.response.IntrospectResponse;
import org.gomsu.identityservice.dto.response.UserResponse;
import org.gomsu.identityservice.service.AuthenticationService;
import org.gomsu.identityservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.text.ParseException;
import com.nimbusds.jose.JOSEException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/token")
    public AuthenticationResponse login(@Valid @RequestBody AuthenticationRequest authenticationRequest) {
        return authenticationService.authenticate(authenticationRequest);
    }

    @PostMapping("/introspect")
    public IntrospectResponse introspect(@RequestBody IntrospectRequest request) {
        return authenticationService.introspect(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody LogoutRequest request)
            throws ParseException, JOSEException {

        authenticationService.logout(request);

        return ResponseEntity.ok("Dang xuat thanh cong!");
    }
}
