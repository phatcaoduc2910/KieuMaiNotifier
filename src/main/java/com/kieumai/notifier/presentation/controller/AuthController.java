package com.kieumai.notifier.presentation.controller;

import com.kieumai.notifier.business.service.AuthService;
import com.kieumai.notifier.business.service.CustomUserDetailsService;
import com.kieumai.notifier.business.service.JwtService;
import com.kieumai.notifier.presentation.dto.LoginRequest;
import com.kieumai.notifier.presentation.dto.LoginResponse;
import com.kieumai.notifier.presentation.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    @Autowired
    private AuthService authService;


    @PostMapping("/auth/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest registerRequest) {
        if (!authService.register(registerRequest)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User đã tồn tại");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body("Đăng kí thành công");
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login (@RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = authService.login(loginRequest);
        if (loginResponse.getToken() != null) {
            return ResponseEntity.ok().body(loginResponse);
        }

        return  ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(loginResponse);
    }
}
