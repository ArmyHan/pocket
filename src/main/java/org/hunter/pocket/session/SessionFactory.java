package org.hunter.pocket.session;

import org.hunter.pocket.config.DatabaseConfig;
import org.hunter.pocket.config.DatabaseNodeConfig;
import org.hunter.pocket.constant.CommonSql;
import org.hunter.pocket.constant.DatasourceDriverTypes;
import org.hunter.pocket.exception.SessionException;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wujianchuan 2018/12/31
 */
public class SessionFactory {
    private static final Map<String, DatabaseNodeConfig> NODE_POOL = new ConcurrentHashMap<>(5);
    private static final Map<String, CacheHolder> CACHE_POOL = new ConcurrentHashMap<>(5);

    private SessionFactory() {
    }

    public static void register(DatabaseConfig databaseConfig) {
        databaseConfig.getNode().forEach(databaseNodeConfig -> {
            if (DatasourceDriverTypes.MYSQL_DRIVER.equals(databaseNodeConfig.getDriverName()) || DatasourceDriverTypes.ORACLE_DRIVER.equals(databaseNodeConfig.getDriverName())) {
                Arrays.stream(databaseNodeConfig.getSession().split(CommonSql.COMMA))
                        .forEach(sessionName -> {
                            if (!NODE_POOL.containsKey(sessionName)) {
                                NODE_POOL.put(sessionName, databaseNodeConfig);
                                Integer cacheSize = databaseNodeConfig.getCacheSize();
                                if (cacheSize == null) {
                                    cacheSize = 100;
                                }
                                CACHE_POOL.put(sessionName, new CacheHolder(cacheSize));
                            } else {
                                throw new SessionException("Session name duplicate.");
                            }
                        });
            } else {
                throw new SessionException("I'm sorry about that I don't support this database now.");
            }
        });
    }

    /**
     * 新建一个session对象
     *
     * @param sessionName session name
     * @return session
     */
    public static Session getSession(String sessionName) {
        return new SessionImpl(NODE_POOL.get(sessionName), sessionName);
    }

    /**
     * 获取session对应的缓存空间
     *
     * @param sessionName session name
     * @return cache
     */
    public static CacheHolder getCache(String sessionName) {
        return CACHE_POOL.get(sessionName);
    }
}
