package com.example.lcms.service;

import com.example.lcms.config.CustomUserDetails;
import com.example.lcms.entity.User;
import com.example.lcms.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // userRepository를 사용해서 데이터베이스에서 username을 가진 회원을 찾습니다.
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("해당 사용자를 찾을 수 없습니다: " + username));

        // 찾은 User 엔티티를 CustomUserDetails 객체로 변환하여 반환합니다.
        return new CustomUserDetails(user);
    }
}