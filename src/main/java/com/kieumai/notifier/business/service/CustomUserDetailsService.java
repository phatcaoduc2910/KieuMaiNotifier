package com.kieumai.notifier.business.service;

import com.kieumai.notifier.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByUsername(username);
        if (user == null) throw new UsernameNotFoundException("User does not exist.");
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPasswordHash()).roles("USER").build();
    }
}
