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

package stroom.job.impl;

import stroom.job.shared.FindJobAction;
import stroom.job.shared.Job;
import stroom.task.api.AbstractTaskHandler;
import stroom.util.shared.BaseResultList;
import stroom.util.shared.ResultList;

import javax.inject.Inject;

class FindJobHandler extends AbstractTaskHandler<FindJobAction, ResultList<Job>> {
    private final JobService jobService;

    @Inject
    FindJobHandler(final JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public BaseResultList<Job> exec(final FindJobAction action) {
        return jobService.find(action.getCriteria());
    }
}