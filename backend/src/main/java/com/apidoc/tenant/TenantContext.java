package com.apidoc.tenant;

import lombok.extern.slf4j.Slf4j;

/**
 * 租户上下文管理器
 * 使用ThreadLocal存储当前线程的租户ID
 * 支持多租户数据库隔离
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();
    private static final Long DEFAULT_TENANT_ID = 1L;

    /**
     * 设置当前租户ID
     */
    public static void setTenantId(Long tenantId) {
        if (tenantId == null) {
            log.warn("尝试设置空租户ID，使用默认值");
            tenantId = DEFAULT_TENANT_ID;
        }
        CURRENT_TENANT.set(tenantId);
        log.debug("设置租户上下文: tenantId={}", tenantId);
    }

    /**
     * 获取当前租户ID
     */
    public static Long getTenantId() {
        Long tenantId = CURRENT_TENANT.get();
        return tenantId != null ? tenantId : DEFAULT_TENANT_ID;
    }

    /**
     * 清除租户上下文
     */
    public static void clear() {
        CURRENT_TENANT.remove();
        log.debug("清除租户上下文");
    }

    /**
     * 检查是否设置了租户上下文
     */
    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * 获取默认租户ID
     */
    public static Long getDefaultTenantId() {
        return DEFAULT_TENANT_ID;
    }
}
