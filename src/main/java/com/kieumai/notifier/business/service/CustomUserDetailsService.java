package com.kieumai.notifier.business.service;

import com.kieumai.notifier.persistence.entity.User;
import com.kieumai.notifier.persistence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserName(username);

        if (user != null) {
            return org.springframework.security.core.userdetails.User.withUsername(user.getUserName())
                    .password(user.getPassWord()).roles(user.getRole()).build();
        }

        return null;
    }
}
