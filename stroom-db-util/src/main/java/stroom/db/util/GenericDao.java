package stroom.db.util;

import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UpdatableRecord;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.shared.HasCrud;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class GenericDao<RecordType extends UpdatableRecord, ObjectType, IdType>
        implements HasCrud<ObjectType, IdType> {

    private static final LambdaLogger LAMBDA_LOGGER = LambdaLoggerFactory.getLogger(GenericDao.class);

    private Table<RecordType> table;
    private TableField<RecordType, IdType> idField;
    private Class<ObjectType> objectTypeClass;
    private DataSource connectionProvider;
    private BiFunction<ObjectType, RecordType, RecordType> objectToRecordMapper = (object, record) -> {
        record.from(object);
        return record;
    };
    private Function<Record, ObjectType> recordToObjectMapper = record ->
            record.into(objectTypeClass);

    // Could use the pattern described here to get the table type:
    // https://stackoverflow.com/questions/3403909/get-generic-type-of-class-at-runtime
    // That places an interface requirement on the object, which I think is best avoided.
    public GenericDao(@Nonnull final Table<RecordType> table,
                      @Nonnull final TableField<RecordType, IdType> idField,
                      @Nonnull final Class<ObjectType> objectTypeClass,
                      @Nonnull final DataSource connectionProvider) {
        this.table = table;
        this.idField = idField;
        this.objectTypeClass = objectTypeClass;
        this.connectionProvider = connectionProvider;
    }

    public ObjectType create(@Nonnull final ObjectType object) {
        return JooqUtil.contextResult(connectionProvider, context -> {
            LAMBDA_LOGGER.debug(() -> LambdaLogger.buildMessage("Creating a {}", table.getName()));
            final RecordType record = objectToRecordMapper.apply(object, context.newRecord(table));
            record.store();
            return recordToObjectMapper.apply(record);
        });
    }

    public ObjectType update(@Nonnull final ObjectType object) {
        return JooqUtil.contextWithOptimisticLocking(connectionProvider, context -> {
            final RecordType record = objectToRecordMapper.apply(object, context.newRecord(table));
            LAMBDA_LOGGER.debug(() -> LambdaLogger.buildMessage("Updating a {} with id {}", table.getName(), record.get(idField)));
            record.update();
            return recordToObjectMapper.apply(record);
        });
    }

    public boolean delete(@Nonnull final IdType id) {
        LAMBDA_LOGGER.debug(() -> LambdaLogger.buildMessage(
                "Deleting a {} with id {}", table.getName(), id));
        return JooqUtil.contextResult(connectionProvider, context -> context
                .deleteFrom(table)
                .where(idField.eq(id))
                .execute() > 0);
    }

    public Optional<ObjectType> fetch(@Nonnull final IdType id) {
        LAMBDA_LOGGER.debug(() -> LambdaLogger.buildMessage(
                "Fetching {} with id {}", table.getName(), id));
        return JooqUtil.contextResult(connectionProvider, context -> context
                .selectFrom(table)
                .where(idField.eq(id))
                .fetchOptional(record -> recordToObjectMapper.apply(record)));
    }

    public GenericDao<RecordType, ObjectType, IdType> setObjectToRecordMapper(final BiFunction<ObjectType, RecordType, RecordType> objectToRecordMapper) {
        this.objectToRecordMapper = objectToRecordMapper;
        return this;
    }

    public GenericDao<RecordType, ObjectType, IdType> setRecordToObjectMapper(final Function<Record, ObjectType> recordToObjectMapper) {
        this.recordToObjectMapper = recordToObjectMapper;
        return this;
    }
}