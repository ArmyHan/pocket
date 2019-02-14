package org.hunter.pocket.criteria;

import org.hunter.pocket.constant.SqlOperateTypes;
import org.hunter.pocket.model.BaseEntity;
import org.hunter.pocket.annotation.Entity;
import org.hunter.pocket.annotation.OneToMany;
import org.hunter.pocket.config.DatabaseNodeConfig;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wujianchuan 2019/1/10
 */
public class CriteriaImpl extends AbstractCriteria implements Criteria {

    public CriteriaImpl(Class clazz, Connection connection, DatabaseNodeConfig databaseConfig) {
        super(clazz, connection, databaseConfig);
    }

    @Override
    public Criteria add(Restrictions restrictions) {
        if (this.restrictionsList.size() == 0) {
            this.sqlRestriction.append(" WHERE ");
        } else {
            this.sqlRestriction.append(" AND ");
        }
        this.sqlRestriction.append(this.parseSql(restrictions));
        return this;
    }

    @Override
    public Criteria add(Modern modern) {
        this.modernList.add(modern);
        return this;
    }

    @Override
    public Criteria add(Sort order) {
        this.orderList.add(order);
        return this;
    }

    @Override
    public int update() throws Exception {
        completeSql.append("UPDATE ")
                .append(this.tableName)
                .append(" SET ")
                .append(this.modernList.stream()
                        .map(modern -> fieldMapper.get(modern.getSource()).getColumnName() + " = ?")
                        .collect(Collectors.joining(", ")))
                .append(this.sqlRestriction);
        PreparedStatement preparedStatement = getPreparedStatement();
        this.after();
        return preparedStatement.executeUpdate();
    }

    @Override
    public List list() throws Exception {
        completeSql.append("SELECT ")
                .append(this.reflectUtils.getColumnNames(this.fields))
                .append(" FROM ")
                .append(this.tableName)
                .append(this.sqlRestriction);
        if (this.orderList.size() > 0) {
            completeSql.append(" ORDER BY ")
                    .append(this.orderList.stream().map(order -> fieldMapper.get(order.getSource()).getColumnName() + " " + order.getSortType()).collect(Collectors.joining(",")));
        }
        PreparedStatement preparedStatement = getPreparedStatement();
        ResultSet resultSet = preparedStatement.executeQuery();
        List<BaseEntity> result = new ArrayList<>();
        try {
            while (resultSet.next()) {
                BaseEntity entity = (BaseEntity) clazz.newInstance();
                for (Field field : this.fields) {
                    field.setAccessible(true);
                    field.set(entity, this.fieldTypeStrategy.getColumnValue(field, resultSet));
                }
                result.add(entity);
            }
        } finally {
            resultSet.close();
            preparedStatement.close();
            this.after();
        }
        return result;
    }

    @Override
    public List list(boolean cascade) throws Exception {
        List result = this.list();
        if (cascade) {
            if (result.size() > 0) {
                for (Object entity : result) {
                    this.applyChildren((BaseEntity) entity);
                }
            }
        }
        return result;
    }

    @Override
    public Object unique() throws Exception {
        completeSql.append("SELECT ")
                .append(this.reflectUtils.getColumnNames(this.fields))
                .append(" FROM ")
                .append(this.tableName)
                .append(this.sqlRestriction);
        PreparedStatement preparedStatement = getPreparedStatement();
        ResultSet resultSet = preparedStatement.executeQuery();
        BaseEntity entity = (BaseEntity) clazz.newInstance();
        try {
            if (resultSet.next()) {
                for (Field field : this.fields) {
                    field.setAccessible(true);
                    field.set(entity, this.fieldTypeStrategy.getColumnValue(field, resultSet));
                }
            } else {
                return null;
            }
        } finally {
            resultSet.close();
            preparedStatement.close();
            this.after();
        }
        return entity;
    }

    @Override
    public Object unique(boolean cascade) throws Exception {
        BaseEntity obj = (BaseEntity) this.unique();
        if (obj != null && cascade) {
            this.applyChildren(obj);
        }
        this.after();
        return obj;
    }

    @Override
    public long count() throws Exception {
        completeSql.append("SELECT ")
                .append(SqlOperateTypes.COUNT)
                .append("(0)")
                .append(" FROM ")
                .append(this.tableName)
                .append(this.sqlRestriction);
        PreparedStatement preparedStatement = getPreparedStatement();
        ResultSet resultSet = preparedStatement.executeQuery();
        this.after();
        if (resultSet.next()) {
            return (long) resultSet.getObject(1);
        } else {
            throw new RuntimeException("No data found");
        }
    }

    @Override
    public long delete() throws Exception {
        completeSql.append("DELETE FROM ")
                .append(this.tableName)
                .append(this.sqlRestriction);
        PreparedStatement preparedStatement = getPreparedStatement();
        this.after();
        return preparedStatement.executeUpdate();
    }

    @Override
    public Object max(String fieldName) throws Exception {
        completeSql.append("SELECT ")
                .append(SqlOperateTypes.MAX)
                .append("(")
                .append(fieldMapper.get(fieldName).getColumnName())
                .append(") ")
                .append(" FROM ")
                .append(this.tableName)
                .append(this.sqlRestriction);
        PreparedStatement preparedStatement = getPreparedStatement();
        ResultSet resultSet = preparedStatement.executeQuery();
        this.after();
        if (resultSet.next()) {
            return resultSet.getObject(1);
        } else {
            throw new RuntimeException("No data found");
        }
    }

    /**
     * 给实体追加子表信息
     *
     * @param entity 实体
     * @throws Exception 异常
     */
    private void applyChildren(BaseEntity entity) throws Exception {
        if (entity.getUuid() != null && childrenFields.length > 0) {
            for (Field childField : childrenFields) {
                childField.setAccessible(true);
                if (childField.getAnnotation(OneToMany.class) != null) {
                    childField.set(entity, this.getChildren(childField, entity.getUuid()));
                }
            }
        }
    }

    private Collection getChildren(Field field, Long uuid) throws Exception {
        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        Class clazz = oneToMany.clazz();
        String columnName = oneToMany.name();
        Entity entityAnnotation = (Entity) clazz.getAnnotation(Entity.class);
        Field[] fields = reflectUtils.getMappingField(clazz);
        String sql = "SELECT "
                + reflectUtils.getColumnNames(fields)
                + " FROM "
                + entityAnnotation.table()
                + " WHERE "
                + columnName
                + " = ?";

        PreparedStatement preparedStatement = this.connection.prepareStatement(sql);
        preparedStatement.setLong(1, uuid);
        ResultSet resultSet = preparedStatement.executeQuery();
        List<BaseEntity> collection = new ArrayList<>();
        try {
            while (resultSet.next()) {
                BaseEntity entity = (BaseEntity) clazz.newInstance();
                for (Field childField : fields) {
                    childField.setAccessible(true);
                    if (childField.getAnnotation(OneToMany.class) != null) {
                        childField.set(entity, this.getChildren(childField, entity.getUuid()));
                    } else {
                        childField.set(entity, fieldTypeStrategy.getColumnValue(childField, resultSet));
                    }
                }
                collection.add(entity);
            }
        } finally {
            resultSet.close();
            preparedStatement.close();
        }
        return collection;
    }

    /**
     * 解析出SQL
     *
     * @param restrictions 约束对象
     * @return SQL
     */
    private String parseSql(Restrictions restrictions) {
        StringBuilder sql = new StringBuilder();
        if (restrictions.getLeftRestrictions() == null) {
            sql.append(fieldMapper.get(restrictions.getSource()).getColumnName())
                    .append(this.sqlFactory.getSql(this.databaseConfig.getDriverName(), restrictions.getSqlOperate()))
                    .append("?");
            this.restrictionsList.add(restrictions);
        } else {
            sql.append("(")
                    .append(this.parseSql(restrictions.getLeftRestrictions()))
                    .append(this.sqlFactory.getSql(this.databaseConfig.getDriverName(), restrictions.getSqlOperate()))
                    .append(this.parseSql(restrictions.getRightRestrictions()))
                    .append(")");
        }
        return sql.toString();
    }

    private PreparedStatement getPreparedStatement() throws SQLException {
        this.before();
        PreparedStatement preparedStatement = this.connection.prepareStatement(completeSql.toString());
        fieldTypeStrategy.setPreparedStatement(preparedStatement, this.modernList, this.restrictionsList);
        return preparedStatement;
    }
}