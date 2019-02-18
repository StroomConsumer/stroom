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

package stroom.receive.rules.impl;

import com.google.inject.AbstractModule;
import stroom.explorer.api.ExplorerActionHandler;
import stroom.importexport.api.ImportExportActionHandler;
import stroom.receive.common.AttributeMapFilterFactory;
import stroom.receive.rules.shared.ReceiveDataRules;
import stroom.util.GuiceUtil;
import stroom.util.RestResource;
import stroom.util.entity.EntityTypeBinder;

public class ReceiveDataRulesetModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ReceiveDataRuleSetService.class).to(ReceiveDataRuleSetServiceImpl.class);
        bind(AttributeMapFilterFactory.class).to(AttributeMapFilterFactoryImpl.class);

        GuiceUtil.buildMultiBinder(binder(), ExplorerActionHandler.class)
                .addBinding(ReceiveDataRuleSetServiceImpl.class);

        GuiceUtil.buildMultiBinder(binder(), ImportExportActionHandler.class)
                .addBinding(ReceiveDataRuleSetServiceImpl.class);

        GuiceUtil.buildMultiBinder(binder(), RestResource.class)
                .addBinding(ReceiveDataRuleSetResource.class)
                .addBinding(ReceiveDataRuleSetResource2.class);

        EntityTypeBinder.create(binder())
                .bind(ReceiveDataRules.DOCUMENT_TYPE, ReceiveDataRuleSetServiceImpl.class);

    }
}