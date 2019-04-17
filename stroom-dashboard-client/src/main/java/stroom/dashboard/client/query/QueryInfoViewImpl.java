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

package stroom.dashboard.client.query;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;
import stroom.dashboard.client.query.QueryInfoPresenter.QueryInfoView;

public class QueryInfoViewImpl extends ViewWithUiHandlers implements QueryInfoView {
    public interface Binder extends UiBinder<Widget, QueryInfoViewImpl> {
    }

    @UiField
    TextArea queryInfo;

    private final Widget widget;

    @Inject
    public QueryInfoViewImpl(final Binder binder) {
        widget = binder.createAndBindUi(this);
        widget.addAttachHandler(event -> focus());
    }

    @Override
    public void focus() {
        Scheduler.get().scheduleDeferred(() -> queryInfo.setFocus(true));
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public String getQueryInfo() {
        return queryInfo.getText();
    }

    @Override
    public void setQueryInfo(final String queryInfo) {
        this.queryInfo.setText(queryInfo);
        focus();
    }
}