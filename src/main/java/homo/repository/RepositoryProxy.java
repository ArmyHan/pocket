package homo.repository;


import homo.User;
import homo.common.model.Entity;
import homo.constant.OperateTypes;
import homo.observe.evens.RepositoryEven;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wujianchuan 2018/12/26
 */
public class RepositoryProxy<T extends Entity> implements HomoRepository<T> {

    private ApplicationContext context;
    private AbstractRepository<T> repository;

    void setRepository(ApplicationContext context, AbstractRepository<T> repository) {
        this.repository = repository;
        this.context = context;
    }

    @Override
    public int save(T entity, User operator) {
        int affected = this.repository.save(entity, operator);
        this.afterReturningAdvise(entity, OperateTypes.SAVE, operator);
        return affected;
    }

    @Override
    public int update(T entity, User operator) {
        int affected = this.repository.update(entity, operator);
        this.afterReturningAdvise(entity, OperateTypes.UPDATE, operator);
        return affected;
    }

    @Override
    public int delete(T entity, User operator) {
        int affected = this.repository.delete(entity, operator);
        this.afterReturningAdvise(entity, OperateTypes.DELETE, operator);
        return affected;
    }

    private void afterReturningAdvise(T entity, OperateTypes operateType, User operator) {
        Class clazz = entity.getClass();
        Map<String, Object> source = new HashMap<>(2);
        source.put("clazz", clazz);
        source.put("entity", entity);
        source.put("operator", operator);
        source.put("operateType", operateType);
        context.publishEvent(new RepositoryEven(source));
    }
}
