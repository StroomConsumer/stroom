package stroom.annotation.impl.db;

import com.zaxxer.hikari.HikariConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.annotation.impl.AnnotationConfig;
import stroom.annotation.impl.AnnotationDao;
import stroom.annotation.impl.AnnotationModule;
import stroom.db.util.AbstractFlyWayDbModule;
import stroom.util.guice.GuiceUtil;

import javax.sql.DataSource;
import java.util.function.Function;

public class AnnotationDbModule extends AbstractFlyWayDbModule<AnnotationConfig, AnnotationDbConnProvider> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationDbModule.class);
    private static final String MODULE = "stroom-annotation";
    private static final String FLYWAY_LOCATIONS = "stroom/annotation/impl/db/migration";
    private static final String FLYWAY_TABLE = "annotation_schema_history";

    @Override
    protected void configure() {
        install(new AnnotationModule());

        bind(AnnotationDao.class).to(AnnotationDaoImpl.class);

        // MultiBind the connection provider so we can see status for all databases.
        GuiceUtil.buildMultiBinder(binder(), DataSource.class)
                .addBinding(AnnotationDbConnProvider.class);
    }

    @Override
    public String getFlyWayTableName() {
        return FLYWAY_TABLE;
    }

    @Override
    public String getModuleName() {
        return MODULE;
    }

    @Override
    public String getFlyWayLocation() {
        return FLYWAY_LOCATIONS;
    }

    @Override
    public Function<HikariConfig, AnnotationDbConnProvider> getConnectionProviderConstructor() {
        return AnnotationDbConnProvider::new;
    }

    @Override
    public Class<AnnotationDbConnProvider> getConnectionProviderType() {
        return AnnotationDbConnProvider.class;
    }
}