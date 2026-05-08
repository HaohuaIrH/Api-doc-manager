package com.apidoc.tenant;

import com.apidoc.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 租户切面
 * 自动在Repository方法执行前后设置和清除租户上下文
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantAspect {

    /**
     * 为所有Repository方法自动设置租户上下文
     */
    @Around("execution(* com.apidoc.repository.*.*(..))")
    public Object setTenantContext(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            // 如果租户上下文未设置，尝试从安全上下文获取
            if (!TenantContext.hasTenant()) {
                Long userId = SecurityContextHelper.getCurrentUserId();
                if (userId != null) {
                    // 对于用户相关操作，使用用户ID作为租户标识
                    TenantContext.setTenantId(userId);
                } else {
                    TenantContext.setTenantId(TenantContext.getDefaultTenantId());
                }
            }

            return joinPoint.proceed();
        } finally {
            // 重要：不要在这里清除上下文，因为可能有级联调用
            // 上下文清理由TenantInterceptor在请求完成后处理
        }
    }
}
