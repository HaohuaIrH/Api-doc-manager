package com.apidoc.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租户过滤器
 * 确保每个请求都有正确的租户上下文
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String DEFAULT_TENANT_VALUE = "1";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 提取租户ID
            String tenantIdStr = request.getHeader(TENANT_HEADER);
            Long tenantId = parseTenantId(tenantIdStr);

            // 设置租户上下文
            TenantContext.setTenantId(tenantId);

            log.debug("请求 {} 设置租户上下文: {}", request.getRequestURI(), tenantId);

            // 继续处理请求
            filterChain.doFilter(request, response);

        } finally {
            // 请求完成后清除租户上下文
            TenantContext.clear();
        }
    }

    /**
     * 解析租户ID
     */
    private Long parseTenantId(String tenantIdStr) {
        if (tenantIdStr == null || tenantIdStr.trim().isEmpty()) {
            return TenantContext.getDefaultTenantId();
        }

        try {
            return Long.parseLong(tenantIdStr.trim());
        } catch (NumberFormatException e) {
            log.warn("无效的租户ID格式: {}，使用默认值", tenantIdStr);
            return TenantContext.getDefaultTenantId();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 对于静态资源跳过过滤
        String path = request.getRequestURI();
        return path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.contains("swagger") ||
               path.contains("api-docs");
    }
}
