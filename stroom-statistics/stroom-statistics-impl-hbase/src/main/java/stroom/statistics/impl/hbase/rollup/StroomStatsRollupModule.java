/*
 * Copyright 2018 Crown Copyright
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

package stroom.statistics.impl.hbase.rollup;

import com.google.inject.AbstractModule;
import stroom.statistics.impl.hbase.shared.StroomStatsRollUpBitMaskConversionAction;
import stroom.statistics.impl.hbase.shared.StroomStatsRollUpBitMaskPermGenerationAction;
import stroom.statistics.impl.hbase.shared.StroomStatsStoreFieldChangeAction;
import stroom.task.api.TaskHandlerBinder;

public class StroomStatsRollupModule extends AbstractModule {
    @Override
    protected void configure() {
        TaskHandlerBinder.create(binder())
                .bind(StroomStatsRollUpBitMaskConversionAction.class, StroomStatsRollUpBitMaskConversionHandler.class)
                .bind(StroomStatsRollUpBitMaskPermGenerationAction.class, StroomStatsRollUpBitMaskPermGenerationHandler.class)
                .bind(StroomStatsStoreFieldChangeAction.class, StroomStatsStoreFieldChangeHandler.class);
    }
}