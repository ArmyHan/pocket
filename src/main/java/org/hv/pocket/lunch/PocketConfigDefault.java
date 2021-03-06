package org.hv.pocket.lunch;

import org.hv.pocket.config.DatabaseConfig;
import org.hv.pocket.connect.ConnectionManager;
import org.hv.pocket.exception.PocketMapperException;
import org.hv.pocket.identify.IdentifyGenerator;
import org.hv.pocket.identify.IdentifyGeneratorFactory;
import org.hv.pocket.model.MapperFactory;
import org.hv.pocket.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author wujianchuan 2019/1/12
 */
@Component
public class PocketConfigDefault implements PocketConfig {
    private final DatabaseConfig databaseConfig;
    private final ApplicationContext context;
    private final List<IdentifyGenerator> identifyGeneratorList;

    @Autowired
    public PocketConfigDefault(DatabaseConfig databaseConfig, List<IdentifyGenerator> identifyGeneratorList, ApplicationContext context) {
        this.databaseConfig = databaseConfig;
        this.identifyGeneratorList = identifyGeneratorList;
        this.context = context;
    }

    @Override
    public void init() throws PocketMapperException {
        this.initConnectionManager();
        this.initSessionFactory();
        this.initIdentifyGenerator();
        MapperFactory.init(context);
    }

    private void initConnectionManager() {
        ConnectionManager connectionManager = ConnectionManager.getInstance();
        connectionManager.register(databaseConfig);
    }

    private void initSessionFactory() {
        SessionFactory.register(databaseConfig);
    }

    private void initIdentifyGenerator() {
        IdentifyGeneratorFactory identifyGeneratorFactory = IdentifyGeneratorFactory.getInstance();
        this.identifyGeneratorList.forEach(identifyGenerator -> {
            identifyGenerator.setGeneratorId();
            identifyGeneratorFactory.registerGenerator(identifyGenerator);
        });
    }
}
