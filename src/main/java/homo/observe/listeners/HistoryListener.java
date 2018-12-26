package homo.observe.listeners;

import homo.model.Entity;
import homo.observe.evens.ModelSaveEven;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author wujianchuan 2018/12/26
 */
@Component
public class HistoryListener implements SmartApplicationListener {
    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return ModelSaveEven.class == eventType;
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        Entity entity = (Entity) event.getSource();
        System.out.println("保存" + entity.getUuid() + "的历史数据。");
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
