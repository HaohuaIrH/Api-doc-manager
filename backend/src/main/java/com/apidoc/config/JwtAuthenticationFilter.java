package com.apidoc.config;

import com.apidoc.entity.User;
import com.apidoc.repository.UserRepository;
import com.apidoc.security.SecurityContextHelper;
import com.apidoc.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 * 支持无token请求通过，由业务层判断是否需要认证
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 清除上一次的ThreadLocal数据
        SecurityContextHelper.clear();

        // 创建匿名认证，允许请求通过但不带用户信息
        UsernamePasswordAuthenticationToken anonymousToken =
            new UsernamePasswordAuthenticationToken("anonymous", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(anonymousToken);

        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                final String jwt = authHeader.substring(7);
                log.debug("Processing JWT token");

                // 提取用户名
                final String username = jwtService.extractUsername(jwt);
                log.debug("Extracted username: {}", username);

                if (username != null) {
                    // 从数据库查找用户
                    userRepository.findByUsername(username).ifPresent(user -> {
                        // 检查用户是否启用
                        if (Boolean.TRUE.equals(user.getEnabled())) {
                            // 设置ThreadLocal（供Service层使用）
                            SecurityContextHelper.setCurrentUserId(user.getId());
                            SecurityContextHelper.setCurrentUsername(user.getUsername());
                            SecurityContextHelper.setCurrentUser(user);

                            // 创建Spring Security认证令牌
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                            );
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            // 设置Spring Security上下文
                            SecurityContextHolder.getContext().setAuthentication(authToken);

                            log.info("User {} authenticated successfully, userId={}", username, user.getId());
                        } else {
                            log.warn("User {} is disabled", username);
                        }
                    });
                }

            } catch (Exception e) {
                log.error("JWT authentication error: {}", e.getMessage());
            }
        } else {
            log.debug("No Bearer token found in request, using anonymous authentication");
        }

        // 继续处理请求（匿名用户会通过SecurityConfig，但在Controller中会被拒绝）
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清除ThreadLocal
            SecurityContextHelper.clear();
        }
    }
}
