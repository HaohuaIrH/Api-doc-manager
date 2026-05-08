package com.apidoc.security;

import com.apidoc.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文帮助类
 * 用于获取当前登录用户信息
 */
public class SecurityContextHelper {

    // ThreadLocal存储当前用户ID，确保在请求链中任何位置都能访问
    private static final ThreadLocal<Long> currentUserIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUsernameHolder = new ThreadLocal<>();
    private static final ThreadLocal<User> currentUserHolder = new ThreadLocal<>();

    /**
     * 设置当前用户ID（由Filter调用）
     */
    public static void setCurrentUserId(Long userId) {
        currentUserIdHolder.set(userId);
    }

    /**
     * 设置当前用户名（由Filter调用）
     */
    public static void setCurrentUsername(String username) {
        currentUsernameHolder.set(username);
    }

    /**
     * 设置当前用户对象（由Filter调用）
     */
    public static void setCurrentUser(User user) {
        currentUserHolder.set(user);
    }

    /**
     * 获取当前登录用户的ID（优先从ThreadLocal获取，备用从SecurityContext获取）
     */
    public static Long getCurrentUserId() {
        // 优先从ThreadLocal获取
        Long userId = currentUserIdHolder.get();
        if (userId != null) {
            return userId;
        }

        // 备用方案：从SecurityContext获取
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId();
        }

        return null;
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        String username = currentUsernameHolder.get();
        if (username != null) {
            return username;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getUsername();
        }

        return null;
    }

    /**
     * 获取当前登录用户
     */
    public static User getCurrentUser() {
        User user = currentUserHolder.get();
        if (user != null) {
            return user;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }

        return null;
    }

    /**
     * 检查是否已认证
     */
    public static boolean isAuthenticated() {
        if (currentUserIdHolder.get() != null) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof User;
    }

    /**
     * 清除当前用户信息（请求结束时调用）
     */
    public static void clear() {
        currentUserIdHolder.remove();
        currentUsernameHolder.remove();
        currentUserHolder.remove();
    }
}
