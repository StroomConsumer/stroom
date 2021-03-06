package stroom.config.global.impl;

import stroom.job.api.ScheduledJobsModule;
import stroom.job.api.RunnableWrapper;

import javax.inject.Inject;

import static stroom.job.api.Schedule.ScheduleType.PERIODIC;

public class GlobalConfigJobsModule extends ScheduledJobsModule {
    @Override
    protected void configure() {
        super.configure();
        bindJob()
                .name("Property Cache Reload")
                .description("Reload properties in the cluster")
                .schedule(PERIODIC, "1m")
                .to(PropertyCacheReload.class);
    }

    private static class PropertyCacheReload extends RunnableWrapper {
        @Inject
        PropertyCacheReload(final GlobalConfigService globalConfigService) {
            super(globalConfigService::updateConfigObjects);
        }
    }
}
