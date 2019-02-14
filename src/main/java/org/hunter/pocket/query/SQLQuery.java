package org.hunter.pocket.query;

import java.sql.SQLException;

/**
 * @author wujianchuan 2019/1/3
 */
public interface SQLQuery {

    /**
     * 单条查询
     *
     * @return 查询结果
     */
    abstract Object unique() throws SQLException;
}