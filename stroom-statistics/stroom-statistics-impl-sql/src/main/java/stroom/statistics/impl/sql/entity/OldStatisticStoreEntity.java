/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.statistics.impl.sql.entity;

import stroom.importexport.migration.DocumentEntity;
import stroom.importexport.shared.ExternalFile;
import stroom.statistics.impl.sql.shared.CustomRollUpMask;
import stroom.statistics.impl.sql.shared.EventStoreTimeIntervalEnum;
import stroom.statistics.impl.sql.shared.StatisticField;
import stroom.statistics.impl.sql.shared.StatisticRollUpType;
import stroom.statistics.impl.sql.shared.StatisticType;
import stroom.statistics.impl.sql.shared.StatisticsDataSourceData;

import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Used for legacy migration
 **/
@Deprecated
public class OldStatisticStoreEntity extends DocumentEntity {
    private static final String ENTITY_TYPE = "StatisticStore";
    // IndexFields names
    private static final String FIELD_NAME_DATE_TIME = "Date Time";
    private static final String FIELD_NAME_VALUE = "Statistic Value";
    private static final String FIELD_NAME_COUNT = "Statistic Count";
    private static final String FIELD_NAME_PRECISION_MS = "Precision ms";


    private static final Map<StatisticType, List<String>> STATIC_FIELDS_MAP = new HashMap<>();
    private static final Long DEFAULT_PRECISION = EventStoreTimeIntervalEnum.HOUR.columnInterval();

    static {
        STATIC_FIELDS_MAP.put(StatisticType.COUNT, Arrays.asList(
                FIELD_NAME_DATE_TIME,
                FIELD_NAME_COUNT,
                FIELD_NAME_PRECISION_MS
        ));
        STATIC_FIELDS_MAP.put(StatisticType.VALUE, Arrays.asList(
                FIELD_NAME_DATE_TIME,
                FIELD_NAME_VALUE,
                FIELD_NAME_COUNT,
                FIELD_NAME_PRECISION_MS
        ));
    }

    private String description;
    private byte pStatisticType;
    private byte pRollUpType;
    private Long precision;
    private boolean enabled;

    private String data;
    private StatisticsDataSourceData statisticsDataSourceDataObject;

    public OldStatisticStoreEntity() {
        this.pStatisticType = StatisticType.COUNT.getPrimitiveValue();
        this.pRollUpType = StatisticRollUpType.NONE.getPrimitiveValue();
        this.precision = DEFAULT_PRECISION;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getType() {
        return ENTITY_TYPE;
    }

    public byte getpStatisticType() {
        return pStatisticType;
    }

    public void setpStatisticType(final byte pStatisticType) {
        this.pStatisticType = pStatisticType;
    }

    public StatisticType getStatisticType() {
        return StatisticType.PRIMITIVE_VALUE_CONVERTER.fromPrimitiveValue(pStatisticType);
    }

    public void setStatisticType(final StatisticType statisticType) {
        this.pStatisticType = statisticType.getPrimitiveValue();
    }

    public byte getpRollUpType() {
        return pRollUpType;
    }

    public void setpRollUpType(final byte pRollUpType) {
        this.pRollUpType = pRollUpType;
    }

    public StatisticRollUpType getRollUpType() {
        return StatisticRollUpType.PRIMITIVE_VALUE_CONVERTER.fromPrimitiveValue(pRollUpType);
    }

    public void setRollUpType(final StatisticRollUpType rollUpType) {
        this.pRollUpType = rollUpType.getPrimitiveValue();
    }

    public Long getPrecision() {
        return precision;
    }

    public void setPrecision(final Long precision) {
        this.precision = precision;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @ExternalFile
    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }

    @XmlTransient
    public StatisticsDataSourceData getStatisticDataSourceDataObject() {
        return statisticsDataSourceDataObject;
    }

    public void setStatisticDataSourceDataObject(final StatisticsDataSourceData statisticDataSourceDataObject) {
        // This is done here as the XML libs in the jdk appear to behave differently to the xerces one.
        // The jdk one respects XmlAccessType.FIELD while xerces does not, so sorting in the setter has
        // no affect now.
        statisticDataSourceDataObject.reOrderStatisticFields();
        this.statisticsDataSourceDataObject = statisticDataSourceDataObject;
    }

    public boolean isValidField(final String fieldName) {
        if (statisticsDataSourceDataObject == null) {
            return false;
        } else if (statisticsDataSourceDataObject.getFields() == null) {
            return false;
        } else if (statisticsDataSourceDataObject.getFields().size() == 0) {
            return false;
        } else {
            return statisticsDataSourceDataObject.getFields().contains(new StatisticField(fieldName));
        }
    }

    public boolean isRollUpCombinationSupported(final Set<String> rolledUpFieldNames) {
        if (rolledUpFieldNames == null || rolledUpFieldNames.isEmpty()) {
            return true;
        }

        if (!rolledUpFieldNames.isEmpty() && getRollUpType().equals(StatisticRollUpType.NONE)) {
            return false;
        }

        if (getRollUpType().equals(StatisticRollUpType.ALL)) {
            return true;
        }

        // rolledUpFieldNames not empty if we get here

        if (statisticsDataSourceDataObject == null) {
            throw new RuntimeException(
                    "isRollUpCombinationSupported called with non-empty list but data source has no statistic fields or custom roll up masks");
        }

        return statisticsDataSourceDataObject.isRollUpCombinationSupported(rolledUpFieldNames);
    }

    public Integer getPositionInFieldList(final String fieldName) {
        return statisticsDataSourceDataObject.getFieldPositionInList(fieldName);
    }

    public List<String> getFieldNames() {
        if (statisticsDataSourceDataObject != null) {
            final List<String> fieldNames = new ArrayList<>();
            for (final StatisticField statisticField : statisticsDataSourceDataObject.getFields()) {
                fieldNames.add(statisticField.getFieldName());
            }
            return fieldNames;
        } else {
            return Collections.emptyList();
        }
    }

    public List<String> getAllFieldNames() {
        List<String> allFieldNames = new ArrayList<>(STATIC_FIELDS_MAP.get(getStatisticType()));
        allFieldNames.addAll(getFieldNames());
        return allFieldNames;
    }

    public int getStatisticFieldCount() {
        return statisticsDataSourceDataObject == null ? 0 : statisticsDataSourceDataObject.getFields().size();
    }

    public List<StatisticField> getStatisticFields() {
        if (statisticsDataSourceDataObject != null) {
            return statisticsDataSourceDataObject.getFields();
        } else {
            return Collections.emptyList();
        }
    }

    public Set<CustomRollUpMask> getCustomRollUpMasks() {
        if (statisticsDataSourceDataObject != null) {
            return statisticsDataSourceDataObject.getCustomRollUpMasks();
        } else {
            return Collections.emptySet();
        }
    }
}
