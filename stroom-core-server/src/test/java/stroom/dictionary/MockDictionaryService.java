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

package stroom.dictionary;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import stroom.dictionary.server.DictionaryService;
import stroom.dictionary.shared.Dictionary;
import stroom.dictionary.shared.FindDictionaryCriteria;
import stroom.entity.server.GenericEntityService;
import stroom.entity.server.MockDocumentEntityService;
import stroom.importexport.server.EntityPathResolver;
import stroom.util.spring.StroomSpringProfiles;

import javax.inject.Inject;

@Profile(StroomSpringProfiles.TEST)
@Component
public class MockDictionaryService extends MockDocumentEntityService<Dictionary, FindDictionaryCriteria>
        implements DictionaryService {
    @Inject
    public MockDictionaryService(final GenericEntityService genericEntityService, final EntityPathResolver entityPathResolver) {
        super(genericEntityService, entityPathResolver);
    }

    @Override
    public Class<Dictionary> getEntityClass() {
        return Dictionary.class;
    }
}
