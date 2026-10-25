package com.kieumai.notifier.business.service;

import com.kieumai.notifier.persistence.entity.User;
import com.kieumai.notifier.persistence.repository.UserRepository;
import com.kieumai.notifier.presentation.dto.LoginRequest;
import com.kieumai.notifier.presentation.dto.LoginResponse;
import com.kieumai.notifier.presentation.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtService jwtService;

    public boolean register (RegisterRequest registerRequest) {
        if (userRepository.findByUserName(registerRequest.getUsername()) != null) {
            return false;
        }

        String displayName = registerRequest.getDisplay_name();

        if (displayName == null || displayName.isBlank()) {
            displayName = registerRequest.getUsername();
        }

        User user = new User(registerRequest.getUsername(), passwordEncoder.encode(registerRequest.getPassword()),"USER",displayName,null);

        userRepository.save(user);
        return true;
    }

    public LoginResponse login (LoginRequest loginRequest) {
        try {
            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();

            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);

            authenticationManager.authenticate(token);
            String jwtToken = jwtService.generateToken(loginRequest.getUsername());

            LoginResponse loginResponse = new LoginResponse(jwtToken, "Đăng nhập thành công");
            return loginResponse;
        } catch (AuthenticationException e) {
            LoginResponse loginResponse = new LoginResponse(null, "username hoặc password không hợp lệ");
            return loginResponse;
        }
    }

    public User findByUsername(String username) {
        return userRepository.findByUserName(username);
    }

    public User getInfoUser () {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUserName(username);
        return user;

    }
}
