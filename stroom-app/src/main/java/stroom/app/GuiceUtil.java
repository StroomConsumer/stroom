package stroom.app;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.util.HasHealthCheck;
import stroom.util.RestResource;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.http.HttpSessionListener;
import java.util.EnumSet;
import java.util.Set;

public class GuiceUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuiceUtil.class);
    private static final String HEALTH_CHECK_SUFFIX = "HealthCheck";

//    public static void addHealthCheck(final HealthCheckRegistry healthCheckRegistry,
//                                      final Injector injector,
//                                      final Class<? extends HasHealthCheck> clazz) {
//        final HasHealthCheck hasHealthCheck = injector.getInstance(clazz);
//        addHealthCheck(healthCheckRegistry, hasHealthCheck);
//    }

    public static void addHealthCheck(final HealthCheckRegistry healthCheckRegistry, HasHealthCheck hasHealthCheck) {
        String name = hasHealthCheck.getClass().getName() + HEALTH_CHECK_SUFFIX;
        LOGGER.info("Registering health check {}", name);
        healthCheckRegistry.register(name, hasHealthCheck.getHealthCheck());
    }

    public static FilterHolder addFilter(final ServletContextHandler servletContextHandler,
                                         final Injector injector,
                                         final Class<? extends Filter> clazz,
                                         final String url) {
        final Filter filter = injector.getInstance(clazz);
        final FilterHolder filterHolder = new FilterHolder(filter);
        filterHolder.setName(clazz.getSimpleName());
        servletContextHandler.addFilter(filterHolder, url, EnumSet.of(DispatcherType.REQUEST));
        return filterHolder;
    }

//    public static void addFilterViaProxy(final ServletContextHandler servletContextHandler, final Injector injector, final Class<?> clazz, final String url) {
//        // Check that we can create an object and it is if the right type.
//        try {
//            final Object object = injector.getInstance(clazz);
//            if (!(object instanceof Filter)) {
//                throw new IllegalArgumentException("Expected filter for object " + clazz.getName());
//            }
//        } catch (final RuntimeException e) {
//            LOGGER.error(e.getMessage(), e);
//            throw e;
//        }
//
//        final Provider<Filter> provider = () -> (Filter) injector.getInstance(clazz);
//        final FilterProxy filterProxy = new FilterProxy(provider);
//        final FilterHolder filterHolder = new FilterHolder(filterProxy);
//        filterHolder.setName(clazz.getSimpleName());
//        servletContextHandler.addFilter(filterHolder, url, EnumSet.of(DispatcherType.REQUEST));
//    }

    public static ServletHolder addServlet(final ServletContextHandler servletContextHandler,
                                           final Injector injector,
                                           final Class<?> clazz,
                                           final String url,
                                           final HealthCheckRegistry healthCheckRegistry) {
        final Object object = injector.getInstance(clazz);
        if (!(object instanceof Servlet)) {
            throw new IllegalArgumentException("Expected servlet for object " + clazz.getName());
        }

        String healthCheckName = clazz.getName();
        if (object instanceof HasHealthCheck) {
            // object has a getHealth method so build a HealthCheck that wraps it and
            // adds in the servlet path information
            healthCheckRegistry.register(healthCheckName, new HealthCheck() {
                @Override
                protected Result check() {
                    HealthCheck.Result result = ((HasHealthCheck) object).getHealth();

                    HealthCheck.ResultBuilder resultBuilder = HealthCheck.Result.builder();
                    if (result.getDetails() != null) {
                        result.getDetails().forEach(resultBuilder::withDetail);
                        resultBuilder.withDetail("path", url);
                    }
                    if (result.getMessage() != null) {
                        resultBuilder.withMessage(result.getMessage());
                    }
                    if (result.getError() != null) {
                        resultBuilder.unhealthy(result.getError());
                    } else {
                        if (result.isHealthy()) {
                            resultBuilder.healthy();
                        } else {
                            resultBuilder.unhealthy();
                        }
                    }
                    return resultBuilder.build();
                }
            });
        } else {
            // Servlet doesn't have a health check so create a noddy one that shows the path
            healthCheckRegistry.register(healthCheckName, new HealthCheck() {
                @Override
                protected Result check() {
                    return Result.builder()
                            .healthy()
                            .withDetail("path", url)
                            .build();
                }
            });
        }

        // Now add the servlet
        final ServletHolder servletHolder = new ServletHolder(clazz.getSimpleName(), (Servlet) object);
        servletContextHandler.addServlet(servletHolder, url);
        LOGGER.info("Adding servlet {} on path {}", clazz.getSimpleName(), url);
        return servletHolder;
    }

//    public static void addServletViaProxy(final ServletContextHandler servletContextHandler, final Injector injector, final Class<?> clazz, final String url) {
//        // Check that we can create a object and it is if the right type.
//        try {
//            final Object object = injector.getInstance(clazz);
//            if (!(object instanceof Servlet)) {
//                throw new IllegalArgumentException("Expected servlet for object " + clazz.getName());
//            }
//        } catch (final RuntimeException e) {
//            LOGGER.error(e.getMessage(), e);
//            throw e;
//        }
//
//        final Provider<Servlet> provider = () -> (Servlet) injector.getInstance(clazz);
//        final ServletProxy servletProxy = new ServletProxy(provider);
//        final ServletHolder servletHolder = new ServletHolder(clazz.getSimpleName(), servletProxy);
//        servletContextHandler.addServlet(servletHolder, url);
//    }

    public static void addServletListener(final ServletEnvironment servletEnvironment,
                                          final Injector injector,
                                          final Class<? extends HttpSessionListener> clazz) {
        final HttpSessionListener httpSessionListener = injector.getInstance(clazz);
        servletEnvironment.addServletListeners(httpSessionListener);
    }

    public static void addResource(final JerseyEnvironment jersey,
                                   final Injector injector,
                                   final Class<?> clazz) {
        final Object resource = injector.getInstance(clazz);
        jersey.register(Preconditions.checkNotNull(resource));
    }

    public static void addRestResources(final JerseyEnvironment jersey,
                                        final Injector injector) {

        final Set<RestResource> restResources = stroom.util.GuiceUtil.getMultibinderInstance(
                injector, RestResource.class);

        restResources.forEach(jersey::register);
    }

    public static void manage(final LifecycleEnvironment lifecycleEnvironment,
                              final Injector injector,
                              final Class<? extends Managed> clazz) {
        final Managed managed = injector.getInstance(clazz);
        lifecycleEnvironment.manage(managed);
    }

//    private static class FilterProxy implements Filter {
//        private final Provider<Filter> provider;
//
//        public FilterProxy(final Provider<Filter> provider) {
//            this.provider = provider;
//        }
//
//        @Override
//        public void init(final FilterConfig filterConfig) throws ServletException {
//            provider.get().init(filterConfig);
//        }
//
//        @Override
//        public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
//            provider.get().doFilter(servletRequest, servletResponse, filterChain);
//        }
//
//        @Override
//        public void destroy() {
//            provider.get().destroy();
//        }
//    }
//
//    private static class ServletProxy implements Servlet {
//        private final Provider<Servlet> provider;
//
//        public ServletProxy(final Provider<Servlet> provider) {
//            this.provider = provider;
//        }
//
//        @Override
//        public void init(final ServletConfig servletConfig) throws ServletException {
//            provider.get().init(servletConfig);
//        }
//
//        @Override
//        public ServletConfig getServletConfig() {
//            return provider.get().getServletConfig();
//        }
//
//        @Override
//        public void service(final ServletRequest servletRequest, final ServletResponse servletResponse) throws ServletException, IOException {
//            provider.get().service(servletRequest, servletResponse);
//        }
//
//        @Override
//        public String getServletInfo() {
//            return provider.get().getServletInfo();
//        }
//
//        @Override
//        public void destroy() {
//            provider.get().destroy();
//        }
//    }
}
