package com.kieumai.notifier.business.service;

import com.kieumai.notifier.persistence.entity.User;
import com.kieumai.notifier.persistence.repository.UserRepository;
import com.kieumai.notifier.presentation.auth.UnauthenticatedException;
import com.kieumai.notifier.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public boolean register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()) != null) return false;
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName() == null || request.getDisplayName().isBlank()
                ? request.getUsername() : request.getDisplayName().strip());
        userRepository.save(user);
        return true;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            return new LoginResponse(jwtService.generateToken(request.getUsername()), "Đăng nhập thành công");
        } catch (AuthenticationException exception) {
            return new LoginResponse(null, "username hoặc password không hợp lệ");
        }
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User updateLocation(long userId, BigDecimal latitude, BigDecimal longitude) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthenticatedException("User no longer exists."));
        user.setLatitude(latitude);
        user.setLongitude(longitude);
        return userRepository.save(user);
    }
}
