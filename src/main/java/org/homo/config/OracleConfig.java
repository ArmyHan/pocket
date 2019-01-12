package org.homo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author wujianchuan 2019/1/12
 */
@Component
@ConfigurationProperties(prefix = "datasource.oracle")
public class OracleConfig extends AbstractDatabaseConfig {
}