package com.apidoc.service;

import com.apidoc.dto.AuthResponse;
import com.apidoc.dto.LoginRequest;
import com.apidoc.entity.User;
import com.apidoc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 认证服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     */
    public AuthResponse login(LoginRequest request) {
        // 查找用户
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 检查用户是否启用
        if (!user.getEnabled()) {
            throw new RuntimeException("用户已被禁用");
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 生成JWT token
        String role = user.getRole() != null ? user.getRole().name() : "USER";
        String token = jwtService.generateToken(user.getUsername(), user.getId(), role);

        log.info("用户 {} 登录成功", user.getUsername());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationInSeconds())
                .userId(user.getId())
                .username(user.getUsername())
                .role(role)
                .build();
    }

    /**
     * 用户注册
     */
    public AuthResponse register(String username, String password, String email, String fullName) {
        // 检查用户名是否存在
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否存在
        if (email != null && userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 创建新用户
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .fullName(fullName)
                .role(User.UserRole.USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        // 生成JWT token
        String role = user.getRole() != null ? user.getRole().name() : "USER";
        String token = jwtService.generateToken(user.getUsername(), user.getId(), role);

        log.info("用户 {} 注册成功", user.getUsername());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationInSeconds())
                .userId(user.getId())
                .username(user.getUsername())
                .role(role)
                .build();
    }
}
