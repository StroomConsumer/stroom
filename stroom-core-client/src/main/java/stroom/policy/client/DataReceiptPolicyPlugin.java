/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.policy.client;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import stroom.core.client.ContentManager;
import stroom.core.client.MenuKeys;
import stroom.menubar.client.event.BeforeRevealMenubarEvent;
import stroom.monitoring.client.MonitoringPlugin;
import stroom.policy.shared.Policy;
import stroom.security.client.ClientSecurityContext;
import stroom.policy.client.presenter.DataReceiptPolicyPresenter;
import stroom.svg.client.SvgPresets;
import stroom.widget.menu.client.presenter.IconMenuItem;
import stroom.widget.menu.client.presenter.MenuItem;

public class DataReceiptPolicyPlugin extends MonitoringPlugin<DataReceiptPolicyPresenter> {
    @Inject
    public DataReceiptPolicyPlugin(final EventBus eventBus, final ContentManager eventManager,
                                   final Provider<DataReceiptPolicyPresenter> presenterProvider, final ClientSecurityContext securityContext) {
        super(eventBus, eventManager, presenterProvider, securityContext);
    }

    @Override
    protected void addChildItems(final BeforeRevealMenubarEvent event) {
        if (getSecurityContext().hasAppPermission(Policy.MANAGE_POLICIES_PERMISSION)) {
            event.getMenuItems().addMenuItem(MenuKeys.TOOLS_MENU, createDataRetentionMenuItem());
        }
    }

    private MenuItem createDataRetentionMenuItem() {
        return new IconMenuItem(50, SvgPresets.FEED, SvgPresets.FEED, "Data Receipt", null, true, () -> open());
    }
}