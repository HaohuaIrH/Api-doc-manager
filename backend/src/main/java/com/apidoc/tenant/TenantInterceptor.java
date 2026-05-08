package com.apidoc.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户拦截器
 * 从HTTP请求头中提取租户ID并设置到租户上下文中
 */
@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String AUTH_HEADER = "Authorization";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 优先从请求头获取租户ID
        String tenantIdHeader = request.getHeader(TENANT_HEADER);

        if (tenantIdHeader != null && !tenantIdHeader.isEmpty()) {
            try {
                Long tenantId = Long.parseLong(tenantIdHeader);
                TenantContext.setTenantId(tenantId);
                log.debug("从请求头设置租户ID: {}", tenantId);
                return true;
            } catch (NumberFormatException e) {
                log.warn("无效的租户ID格式: {}", tenantIdHeader);
            }
        }

        // 对于登录请求，使用默认租户
        String requestUri = request.getRequestURI();
        if (requestUri.contains("/api/auth/login") || requestUri.contains("/api/auth/register")) {
            TenantContext.setTenantId(TenantContext.getDefaultTenantId());
            log.debug("登录请求使用默认租户");
            return true;
        }

        // 其他请求需要有租户上下文，否则使用默认租户
        if (!TenantContext.hasTenant()) {
            TenantContext.setTenantId(TenantContext.getDefaultTenantId());
            log.debug("未设置租户上下文，使用默认租户");
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求完成后清除租户上下文
        TenantContext.clear();
    }
}
