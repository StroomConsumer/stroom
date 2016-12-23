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

package stroom.entity.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import stroom.explorer.shared.ExplorerData;

import java.util.List;

public class ShowCopyEntityDialogEvent extends GwtEvent<ShowCopyEntityDialogEvent.Handler> {
    private static Type<Handler> TYPE;
    private final List<ExplorerData> explorerDataList;

    private ShowCopyEntityDialogEvent(final List<ExplorerData> explorerDataList) {
        this.explorerDataList = explorerDataList;
    }

    public static void fire(final HasHandlers handlers, final List<ExplorerData> explorerDataList) {
        handlers.fireEvent(new ShowCopyEntityDialogEvent(explorerDataList));
    }

    public static Type<Handler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    @Override
    public final Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final Handler handler) {
        handler.onCopy(this);
    }

    public List<ExplorerData> getExplorerDataList() {
        return explorerDataList;
    }

    public interface Handler extends EventHandler {
        void onCopy(final ShowCopyEntityDialogEvent event);
    }
}
