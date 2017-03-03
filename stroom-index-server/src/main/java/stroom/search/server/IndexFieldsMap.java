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

package stroom.search.server;

import stroom.index.shared.IndexField;
import stroom.index.shared.IndexFields;

import java.util.HashMap;

public class IndexFieldsMap extends HashMap<String, IndexField> {
    private static final long serialVersionUID = -7687167987530520359L;

    public IndexFieldsMap() {
    }

    public IndexFieldsMap(final IndexFields indexFields) {
        for (final IndexField indexField : indexFields.getIndexFields()) {
            put(indexField);
        }
    }

    public void put(final IndexField indexField) {
        put(indexField.getFieldName(), indexField);
    }
}