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

package stroom.search.solr.search;

import stroom.cluster.task.api.ClusterTaskTerminator;
import stroom.query.api.v2.Query;
import stroom.search.solr.CachedSolrIndex;
import stroom.search.solr.SolrIndexCache;
import stroom.search.solr.shared.SolrIndexField;
import stroom.security.api.SecurityContext;
import stroom.task.api.TaskContext;
import stroom.task.api.TaskManager;
import stroom.task.shared.TaskId;

import javax.inject.Inject;

public class SolrAsyncSearchTaskHandler {
    private final SolrIndexCache solrIndexCache;
    private final SecurityContext securityContext;
    private final SolrClusterSearchTaskHandler clusterSearchTaskHandler;
    private final TaskManager taskManager;
    private final ClusterTaskTerminator clusterTaskTerminator;

    @Inject
    SolrAsyncSearchTaskHandler(final SolrIndexCache solrIndexCache,
                               final SecurityContext securityContext,
                               final SolrClusterSearchTaskHandler clusterSearchTaskHandler,
                               final TaskManager taskManager,
                               final ClusterTaskTerminator clusterTaskTerminator) {
        this.solrIndexCache = solrIndexCache;
        this.securityContext = securityContext;
        this.clusterSearchTaskHandler = clusterSearchTaskHandler;
        this.taskManager = taskManager;
        this.clusterTaskTerminator = clusterTaskTerminator;
    }

    public void exec(final TaskContext taskContext, final SolrAsyncSearchTask task) {
        securityContext.secure(() -> securityContext.useAsRead(() -> {
            final SolrSearchResultCollector resultCollector = task.getResultCollector();
            if (!Thread.currentThread().isInterrupted()) {
                try {
                    taskContext.info(() -> task.getSearchName() + " - initialising");
                    final Query query = task.getQuery();

                    // Reload the index.
                    final CachedSolrIndex index = solrIndexCache.get(query.getDataSource());

                    // Get an array of stored index fields that will be used for
                    // getting stored data.
                    // TODO : Specify stored fields based on the fields that all
                    // coprocessors will require. Also
                    // batch search only needs stream and event id stored fields.
                    final String[] storedFields = getStoredFields(index);

                    final SolrClusterSearchTask clusterSearchTask = new SolrClusterSearchTask(index, query, task.getResultSendFrequency(), storedFields,
                            task.getCoprocessorMap(), task.getDateTimeLocale(), task.getNow());
                    clusterSearchTaskHandler.exec(taskContext, clusterSearchTask, resultCollector);

                    // Await completion.
                    resultCollector.awaitCompletion();

                } catch (final RuntimeException e) {
                    resultCollector.getErrorSet().add(e.getMessage());
                } catch (final InterruptedException e) {
                    resultCollector.getErrorSet().add(e.getMessage());

                    // Continue to interrupt this thread.
                    Thread.currentThread().interrupt();
                } finally {
                    taskContext.info(() -> task.getSearchName() + " - complete");

                    // Make sure we try and terminate any child tasks on worker
                    // nodes if we need to.
                    terminateTasks(task, taskContext.getTaskId());

                    // Let the result handler know search has finished.
                    resultCollector.complete();

                    // We need to wait here for the client to keep getting results if
                    // this is an interactive search.
                    taskContext.info(() -> task.getSearchName() + " - staying alive for UI requests");
                }
            }
        }));
    }

    private void terminateTasks(final SolrAsyncSearchTask task, final TaskId taskId) {
        // Terminate this task.
        taskManager.terminate(taskId);

        // We have to wrap the cluster termination task in another task or
        // ClusterDispatchAsyncImpl
        // will not execute it if the parent task is terminated.
        clusterTaskTerminator.terminate(task.getSearchName(), taskId, "SolrAsyncSearchTask");
    }

    private String[] getStoredFields(final CachedSolrIndex index) {
        return index.getFields()
                .stream()
                .filter(SolrIndexField::isStored)
                .map(SolrIndexField::getFieldName)
                .toArray(String[]::new);
    }
}
