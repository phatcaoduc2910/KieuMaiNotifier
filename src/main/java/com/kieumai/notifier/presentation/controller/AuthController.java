package com.kieumai.notifier.presentation.controller;

import com.kieumai.notifier.business.service.AuthService;
import com.kieumai.notifier.business.service.CustomUserDetailsService;
import com.kieumai.notifier.business.service.JwtService;
import com.kieumai.notifier.persistence.entity.User;
import com.kieumai.notifier.presentation.dto.LoginRequest;
import com.kieumai.notifier.presentation.dto.LoginResponse;
import com.kieumai.notifier.presentation.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/get/me")
    public ResponseEntity<User> getInfo () {
        User user = authService.getInfoUser();
        return ResponseEntity.ok().body(user);
    }
}
