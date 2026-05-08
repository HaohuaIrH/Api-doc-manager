package com.apidoc.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 租户路由数据源
 * 根据租户上下文动态切换数据源
 * 支持多Schema隔离模式
 */
@Slf4j
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        Long tenantId = TenantContext.getTenantId();
        String lookupKey = "tenant_" + tenantId;

        log.debug("路由到数据源: {}", lookupKey);
        return lookupKey;
    }
}
