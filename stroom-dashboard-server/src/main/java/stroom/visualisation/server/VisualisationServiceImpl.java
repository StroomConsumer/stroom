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

package stroom.visualisation.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stroom.entity.server.DocumentEntityServiceImpl;
import stroom.entity.server.ObjectMarshaller;
import stroom.entity.server.QueryAppender;
import stroom.entity.server.util.StroomEntityManager;
import stroom.importexport.server.ImportExportHelper;
import stroom.query.api.v2.DocRef;
import stroom.security.SecurityContext;
import stroom.util.spring.StroomSpringProfiles;
import stroom.visualisation.shared.FindVisualisationCriteria;
import stroom.visualisation.shared.Visualisation;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Profile(StroomSpringProfiles.PROD)
@Component("visualisationService")
@Transactional
public class VisualisationServiceImpl extends DocumentEntityServiceImpl<Visualisation, FindVisualisationCriteria>
        implements VisualisationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisualisationServiceImpl.class);

    @Inject
    VisualisationServiceImpl(final StroomEntityManager entityManager,
                             final ImportExportHelper importExportHelper,
                             final SecurityContext securityContext) {
        super(entityManager, importExportHelper, securityContext);
    }

    @Override
    public Class<Visualisation> getEntityClass() {
        return Visualisation.class;
    }

    @Override
    public FindVisualisationCriteria createCriteria() {
        return new FindVisualisationCriteria();
    }

    @Override
    public DocRef copyDocument(final String originalUuid,
                               final String copyUuid,
                               final Map<String, String> otherCopiesByOriginalUuid,
                               final String parentFolderUUID) {
        final DocRef copiedDocRef = super.copyDocument(originalUuid,
                copyUuid,
                otherCopiesByOriginalUuid,
                parentFolderUUID);

        return makeCopyUuidReplacements(copiedDocRef,
                otherCopiesByOriginalUuid,
                Visualisation::getScriptRefXML,
                Visualisation::setScriptRefXML);
    }

    @Override
    protected QueryAppender<Visualisation, FindVisualisationCriteria> createQueryAppender(StroomEntityManager entityManager) {
        return new VisualisationQueryAppender(entityManager);
    }

    @Override
    public Map<DocRef, Set<DocRef>> getDependencies() {
        final Set<DocRef> docs = super.listDocuments();
        return docs.stream().collect(Collectors.toMap(Function.identity(), this::getDependencies));
    }

    private Set<DocRef> getDependencies(final DocRef docRef) {
        try {
            final Visualisation visualisation = loadByUuid(docRef.getUuid());
            if (visualisation.getScriptRef() != null) {
                return Collections.singleton(visualisation.getScriptRef());
            }
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        return Collections.emptySet();
    }

    private static class VisualisationQueryAppender extends QueryAppender<Visualisation, FindVisualisationCriteria> {
        private final ObjectMarshaller<DocRef> docRefMarshaller;

        public VisualisationQueryAppender(final StroomEntityManager entityManager) {
            super(entityManager);
            docRefMarshaller = new ObjectMarshaller<>(DocRef.class);
        }

        @Override
        protected void preSave(final Visualisation entity) {
            super.preSave(entity);
            entity.setScriptRefXML(docRefMarshaller.marshal(entity.getScriptRef()));
        }

        @Override
        protected void postLoad(final Visualisation entity) {
            entity.setScriptRef(docRefMarshaller.unmarshal(entity.getScriptRefXML()));
            super.postLoad(entity);
        }
    }
}