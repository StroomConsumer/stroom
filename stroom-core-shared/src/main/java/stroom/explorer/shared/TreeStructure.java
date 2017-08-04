/*
 * Copyright 2017 Crown Copyright
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
 *
 */

package stroom.explorer.shared;

import stroom.util.shared.SharedObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeStructure implements SharedObject {
    private static final long serialVersionUID = 4459080492974776354L;
    private ExplorerData root;
    private Map<ExplorerData, ExplorerData> parentMap = new HashMap<>();
    private Map<ExplorerData, List<ExplorerData>> childMap = new HashMap<>();

    public TreeStructure() {
        // Default constructor necessary for GWT serialisation.
    }

    public void add(final ExplorerData parent, final ExplorerData child) {
        if (parent == null) {
            root = child;
        }

        parentMap.put(child, parent);
        childMap.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
    }

    public ExplorerData getRoot() {
        return root;
    }

    public ExplorerData getParent(final ExplorerData child) {
        return parentMap.get(child);
    }

    public List<ExplorerData> getChildren(final ExplorerData parent) {
        return childMap.get(parent);
    }
}
