package org.homo.pocket.session;

import org.homo.core.model.BaseEntity;
import org.homo.pocket.connect.ConnectionManager;
import org.homo.pocket.annotation.Column;
import org.homo.pocket.annotation.Entity;
import org.homo.pocket.config.DatabaseNodeConfig;
import org.homo.pocket.criteria.Criteria;
import org.homo.pocket.criteria.CriteriaImpl;
import org.homo.pocket.criteria.Restrictions;
import org.homo.pocket.query.AbstractQuery;
import org.homo.pocket.query.HomoQuery;
import org.homo.pocket.utils.HomoUuidGenerator;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author wujianchuan 2019/1/1
 */
public class SessionImpl extends AbstractSession {

    SessionImpl(DatabaseNodeConfig databaseNodeConfig) {
        super(databaseNodeConfig);
    }

    @Override
    public void open() {
        this.connection = ConnectionManager.getInstance().getConnection(databaseNodeConfig);
    }

    @Override
    public void close() {
        ConnectionManager.getInstance().closeConnection(this.databaseNodeConfig.getNodeName(), this.connection);
    }

    @Override
    public Transaction getTransaction() {
        return new TransactionImpl(this.connection);
    }

    @Override
    public AbstractQuery createSQLQuery(String sql) {
        return new HomoQuery(sql, this.connection);
    }

    @Override
    public Criteria creatCriteria(Class clazz) {
        return new CriteriaImpl(clazz, this.connection, this.databaseNodeConfig);
    }

    @Override
    public Object save(BaseEntity entity) throws Exception {
        Class clazz = entity.getClass();
        String tableName = reflectUtils.getTableName(clazz);

        Field[] fields = reflectUtils.getMappingField(clazz);
        StringBuilder sql = new StringBuilder("INSERT INTO ")
                .append(tableName)
                .append("(")
                .append(reflectUtils.getColumnNames(fields))
                .append(") ");
        StringBuilder valuesSql = new StringBuilder("VALUES(")
                .append(reflectUtils.getColumnPlaceholder(fields))
                .append(") ");
        sql.append(valuesSql);

        this.showSql(sql.toString());
        long uuid = HomoUuidGenerator.getInstance().getUuid(entity.getClass(), this);
        entity.setUuid(uuid);
        PreparedStatement preparedStatement = this.connection.prepareStatement(sql.toString());
        statementApplyValue(entity, fields, preparedStatement);
        preparedStatement.executeUpdate();
        preparedStatement.close();
        this.adoptChildren(entity);
        return this.findOne(entity.getClass(), entity.getUuid());
    }

    @Override
    public Object update(BaseEntity entity) throws Exception {
        Class clazz = entity.getClass();
        String tableName = reflectUtils.getTableName(clazz);
        Object older = this.findOne(clazz, entity.getUuid());
        if (older != null) {
            Field[] fields = reflectUtils.dirtyFieldFilter(entity, older);
            if (fields.length > 0) {
                StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
                for (int index = 0; index < fields.length; index++) {
                    if (index < fields.length - 1) {
                        sql.append(fields[index].getAnnotation(Column.class).name()).append(" = ?, ");
                    } else {
                        sql.append(fields[index].getAnnotation(Column.class).name()).append(" = ? ");
                    }
                }
                sql.append(" WHERE UUID = ?");
                this.showSql(sql.toString());
                PreparedStatement preparedStatement = this.connection.prepareStatement(sql.toString());
                statementApplyValue(entity, fields, preparedStatement);
                preparedStatement.setObject(fields.length + 1, entity.getUuid());
                preparedStatement.executeUpdate();
                preparedStatement.close();
            } else {
                //TODO: 封装异常类型
                throw new RuntimeException("数据未发生变化");
            }
            return this.findOne(entity.getClass(), entity.getUuid());
        } else {
            //TODO: 封装异常类型
            throw new RuntimeException("未找到数据");
        }
    }

    @Override
    public int delete(BaseEntity entity) throws Exception {
        Class clazz = entity.getClass();
        String tableName = reflectUtils.getTableName(clazz);
        Object garbage = this.findOne(clazz, entity.getUuid());
        if (garbage != null) {
            String sql = "DELETE FROM " + tableName + " WHERE UUID = ?";
            this.showSql(sql);
            PreparedStatement preparedStatement = this.connection.prepareStatement(sql);
            preparedStatement.setLong(1, entity.getUuid());
            int effectRow = preparedStatement.executeUpdate();
            preparedStatement.close();
            return effectRow;
        } else {
            //TODO: 封装异常类型
            throw new RuntimeException("未找到数据");
        }
    }

    @Override
    public Object findOne(Class clazz, Long uuid) throws Exception {
        return this.findDirect(clazz, uuid);
    }

    @Override
    public Object findDirect(Class clazz, Long uuid) throws Exception {
        Criteria criteria = this.creatCriteria(clazz);
        criteria.add(Restrictions.equ("uuid", uuid));
        return criteria.unique();
    }

    @Override
    public long getMaxUuid(Class clazz) throws Exception {
        Entity annotation = (Entity) clazz.getAnnotation(Entity.class);
        PreparedStatement preparedStatement = this.connection.prepareStatement("SELECT MAX(UUID) FROM " + annotation.table());
        ResultSet resultSet = preparedStatement.executeQuery();
        long uuid;
        if (resultSet.next()) {
            uuid = resultSet.getLong(1);
        } else {
            uuid = 0;
        }
        resultSet.close();
        preparedStatement.close();
        return uuid;
    }

    @Override
    public void clearCache() {

    }

    @Override
    public void removeCache(BaseEntity entity) {

    }
}