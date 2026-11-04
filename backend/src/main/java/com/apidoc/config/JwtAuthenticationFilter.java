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
 * 仅在携带有效JWT且用户存在并启用时建立认证；其余请求保持未认证，
 * 由SecurityConfig的 authenticated() 规则拒绝（401），不再降级为匿名放行
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
                            log.warn("User {} is disabled, request stays unauthenticated", username);
                        }
                    });
                }

            } catch (Exception e) {
                // 无效/过期JWT：不降级为匿名，保持未认证，由authenticated()规则返回401
                log.warn("Invalid JWT token, request stays unauthenticated: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        } else {
            log.debug("No Bearer token found in request, request stays unauthenticated");
        }

        // 继续处理请求（未认证请求会被SecurityConfig拒绝）
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清除ThreadLocal与安全上下文，避免线程复用串号
            SecurityContextHelper.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
