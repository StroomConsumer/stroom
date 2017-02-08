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

package stroom.node.client.presenter;

import stroom.data.grid.client.DoubleClickEvent;
import stroom.entity.shared.DocRef;
import stroom.entity.shared.DocRefUtil;
import stroom.entity.shared.EntityServiceLoadAction;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenter;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Proxy;

import stroom.alert.client.event.ConfirmEvent;
import stroom.alert.client.presenter.ConfirmCallback;
import stroom.dispatch.client.AsyncCallbackAdaptor;
import stroom.dispatch.client.ClientDispatchAsync;
import stroom.entity.shared.EntityServiceDeleteAction;
import stroom.node.client.view.WrapperView;
import stroom.node.shared.FlushVolumeStatusAction;
import stroom.node.shared.Volume;
import stroom.util.shared.VoidResult;
import stroom.widget.button.client.GlyphButtonView;
import stroom.widget.button.client.GlyphIcons;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.DefaultPopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupView.PopupType;
import stroom.widget.util.client.MySingleSelectionModel;

import java.util.List;

public class ManageVolumesPresenter extends MyPresenter<WrapperView, ManageVolumesPresenter.ManageVolumesProxy> {
    public static final String LIST = "LIST";
    private final VolumeStatusListPresenter volumeStatusListPresenter;
    private final Provider<VolumeEditPresenter> editProvider;
    private final ClientDispatchAsync dispatcher;

    private final GlyphButtonView newButton;
    private final GlyphButtonView openButton;
    private final GlyphButtonView deleteButton;
    private final GlyphButtonView rescanButton;

    @Inject
    public ManageVolumesPresenter(final EventBus eventBus, final WrapperView view, final ManageVolumesProxy proxy,
                                  final VolumeStatusListPresenter volumeStatusListPresenter, final Provider<VolumeEditPresenter> editProvider,
                                  final ClientDispatchAsync dispatcher) {
        super(eventBus, view, proxy);
        this.volumeStatusListPresenter = volumeStatusListPresenter;
        this.editProvider = editProvider;
        this.dispatcher = dispatcher;

        newButton = volumeStatusListPresenter.getView().addButton(GlyphIcons.NEW_ITEM);
        openButton = volumeStatusListPresenter.getView().addButton(GlyphIcons.EDIT);
        deleteButton = volumeStatusListPresenter.getView().addButton(GlyphIcons.DELETE);
        rescanButton = volumeStatusListPresenter.getView().addButton(GlyphIcons.REFRESH);
        rescanButton.setTitle("Rescan Public Volumes");

        view.setView(volumeStatusListPresenter.getView());
    }

    @Override
    protected void onBind() {
        final PopupUiHandlers popupUiHandlers = new DefaultPopupUiHandlers() {
            @Override
            public void onHide(final boolean autoClose, final boolean ok) {
                refresh();
            }
        };

        registerHandler(volumeStatusListPresenter.getSelectionModel().addSelectionHandler(event -> {
            enableButtons();
            if (event.getSelectionType().isDoubleSelect()) {
                open(popupUiHandlers);
            }
        }));
        registerHandler(newButton.addClickHandler(event -> {
            final VolumeEditPresenter editor = editProvider.get();
            editor.addVolume(new Volume(), popupUiHandlers);
        }));
        registerHandler(openButton.addClickHandler(event -> open(popupUiHandlers)));
        registerHandler(deleteButton.addClickHandler(event -> delete()));
        registerHandler(rescanButton.addClickHandler(event -> dispatcher.execute(new FlushVolumeStatusAction(), new AsyncCallbackAdaptor<VoidResult>() {
            @Override
            public void onSuccess(final VoidResult result) {
                // Ignore these results as they don't
                // have all the volumes.
                refresh();
            }
        })));
    }

    private void open(final PopupUiHandlers popupUiHandlers) {
        final Volume volume = volumeStatusListPresenter.getSelectionModel().getSelected();
        if (volume != null) {
            dispatcher.execute(new EntityServiceLoadAction<Volume>(DocRefUtil.create(volume), null),
                    new AsyncCallbackAdaptor<Volume>() {
                        @Override
                        public void onSuccess(final Volume result) {
                            final VolumeEditPresenter editor = editProvider.get();
                            editor.editVolume(result, popupUiHandlers);
                        }
                    });
        }
    }

    private void delete() {
        final List<Volume> list = volumeStatusListPresenter.getSelectionModel().getSelectedItems();
        if (list != null && list.size() > 0) {
            String message = "Are you sure you want to delete the selected volume?";
            if (list.size() > 1) {
                message = "Are you sure you want to delete the selected volumes?";
            }
            ConfirmEvent.fire(ManageVolumesPresenter.this, message,
                    result -> {
                        if (result) {
                            volumeStatusListPresenter.getSelectionModel().clear();
                            for (final Volume volume : list) {
                                final EntityServiceDeleteAction<Volume> action = new EntityServiceDeleteAction<Volume>(
                                        volume);
                                dispatcher.execute(action, new AsyncCallbackAdaptor<Volume>() {
                                    @Override
                                    public void onSuccess(final Volume result) {
                                        refresh();
                                    }
                                });
                            }
                        }
                    });
        }
    }

    @Override
    protected void revealInParent() {
        final PopupSize popupSize = new PopupSize(800, 400, true);
        ShowPopupEvent.fire(this, this, PopupType.CLOSE_DIALOG, null, popupSize, "Manage Volumes", null, null);
    }

    private void enableButtons() {
        final boolean enabled = volumeStatusListPresenter.getSelectionModel().getSelected() != null;
        openButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
    }

    public void refresh() {
        volumeStatusListPresenter.refresh();
    }

    @ProxyCodeSplit
    public interface ManageVolumesProxy extends Proxy<ManageVolumesPresenter> {
    }
}
