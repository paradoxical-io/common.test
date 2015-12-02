package io.paradoxical.common.test.web.runner;

import io.paradoxical.common.test.guice.ModuleOverrider;
import io.paradoxical.common.test.guice.OverridableModule;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.cli.ServerCommand;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.logging.ConsoleAppenderFactory;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ConfigOverride;
import lombok.NonNull;
import net.sourceforge.argparse4j.inf.Namespace;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.net.BindException;
import java.net.URI;
import java.util.List;

public class ServiceTestRunner<TConfiguration extends Configuration, TApplication extends Application<TConfiguration>> implements AutoCloseable {

    private final TestServiceFactory<TApplication> applicationFactory;
    private final String configPath;
    private long port;

    private final TConfiguration configuration;
    private TApplication application;
    private Server jettyServer;

    private Boolean startHttp = true;
    private ServiceTestRunnerConfig testHostConfiguration;
    private boolean stopped;

    public ServiceTestRunner(TestServiceFactory<TApplication> applicationFactory, @NotNull @NonNull TConfiguration configuration, long port) {
        this(applicationFactory, port, configuration, null);
    }

    public ServiceTestRunner(TestServiceFactory<TApplication> applicationFactory, @Nullable String configPath, ConfigOverride... configOverrides) {
        this(applicationFactory, 0, null, configPath, configOverrides);
    }

    private ServiceTestRunner(
            TestServiceFactory<TApplication> applicationFactory,
            long port,
            @Nullable TConfiguration configuration,
            @Nullable String configPath,
            ConfigOverride... configOverrides) {

        this.applicationFactory = applicationFactory;
        this.configPath = configPath;
        this.configuration = configuration;
        this.port = port;

        for (ConfigOverride configOverride : configOverrides) {
            configOverride.addToSystemProperties();
        }
    }

    public ServiceTestRunner<TConfiguration, TApplication> run(ServiceTestRunnerConfig config, List<OverridableModule> overridableModules) {
        this.testHostConfiguration = config;

        startIfRequired(overridableModules);

        return this;
    }

    public ServiceTestRunner<TConfiguration, TApplication> run(ServiceTestRunnerConfig config) {
        return run(config, ImmutableList.of());
    }

    public ServiceTestRunner<TConfiguration, TApplication> run(List<OverridableModule> overridableModules) {
        return run(ServiceTestRunnerConfig.Default, overridableModules);
    }

    public ServiceTestRunner<TConfiguration, TApplication> run() {
        return run(ServiceTestRunnerConfig.Default);
    }

    public ServiceTestRunner<TConfiguration, TApplication> noHttpServer() {
        internalInit(port, ImmutableList.of());

        startHttp = false;

        return this;
    }

    /**
     * Customize the dropwizard bootstrapping code to provide a custom logger, and return the provided config
     * This way we aren't bound to files to pass in configs and can override things in code
     */
    private Bootstrap<TConfiguration> internalInit(long appPort, final List<OverridableModule> overridableModules) {

        application = applicationFactory.createService(overridableModules);

        final Bootstrap<TConfiguration> bootstrap = getBootrapper(application, appPort);

        if (configuration != null) {
            final StaticConfigurationFactory<TConfiguration> staticConfigurationFactory = new StaticConfigurationFactory<TConfiguration>() {
                @Override public TConfiguration provideConfig() {

                    initializeLogging();

                    return configuration;
                }

                private void initializeLogging() {
                    final DefaultLoggingFactory loggingFactory = ((DefaultLoggingFactory) configuration.getLoggingFactory());

                    final ConsoleAppenderFactory testAppender = new ConsoleAppenderFactory();

                    testAppender.setLogFormat(testHostConfiguration.getLogFormat());

                    loggingFactory.setAppenders(ImmutableList.of(testAppender));
                }
            };

            bootstrap.setConfigurationFactoryFactory((klass, validator, objectMapper, propertyPrefix) -> staticConfigurationFactory);
        }

        application.initialize(bootstrap);

        return bootstrap;
    }

    private void startIfRequired(final List<OverridableModule> overridableModules) {
        if (jettyServer != null) {
            return;
        }
        int retry = 0;

        Exception cause = null;

        // retry if failures happen due to random port bindings
        for (; retry < 10; retry++) {
            try {
                final Bootstrap<TConfiguration> bootstrap = internalInit(port, overridableModules);

                final EnvironmentCommand<TConfiguration> command = getStartupCommand(application);

                ImmutableMap.Builder<String, Object> file = ImmutableMap.builder();

                if (!Strings.isNullOrEmpty(configPath)) {
                    file.put("file", configPath);
                }

                final Namespace namespace = new Namespace(file.build());

                command.run(bootstrap, namespace);

                return;
            }
            catch (Exception ex) {
                cause = ex;

                this.port++;

                if (ex instanceof BindException) {
                    continue;
                }

                if (ex.getCause() != null && ex.getCause() instanceof BindException) {
                    continue;
                }

                throw new RuntimeException(ex);
            }
        }

        if (cause != null) {
            throw new RuntimeException(cause);
        }
    }


    /**
     * Dynamically determine which startup command to choose. If no http return a fake startup command
     * This initializes the bootstrapper and any downstream environment stage (mostly for Guice's bootstrapper to get
     * access to the dropwizard configuration).
     */
    private EnvironmentCommand<TConfiguration> getStartupCommand(final TApplication application) {
        if (startHttp) {
            return new ServerCommand<>(application);
        }

        return new EnvironmentCommand<TConfiguration>(application, "non-running", "test") {
            @Override protected void run(final Environment environment, final Namespace namespace, final TConfiguration configuration) throws Exception {

            }
        };
    }

    /**
     * Get an instance to the web client for this test runner
     *
     * @param path
     * @return
     */

    public WebTarget getClient(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return ClientBuilder.newClient().target("http://localhost:" + getLocalPort()).path(path);
    }

    /**
     * Get an instance to the web client for this test runner
     *
     * @param path
     * @return
     */
    public WebTarget getClient(URI host, String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return ClientBuilder.newClient().target(host.toString()).path(path);
    }

    public TConfiguration getConfiguration() {
        try {
            return application.getConfigurationClass().newInstance();
        }
        catch (Exception ex) {
            return null;
        }
    }

    public int getLocalPort() {
        return ((ServerConnector) jettyServer.getConnectors()[0]).getLocalPort();
    }

    public int getAdminPort() {
        return ((ServerConnector) jettyServer.getConnectors()[1]).getLocalPort();
    }

    @SuppressWarnings("unchecked")
    public TApplication getApplication() {
        return application;
    }

    @Override public void close() throws Exception {
        if (stopped) {
            return;
        }

        try {
            if (jettyServer != null && jettyServer.isRunning()) {
                try {
                    jettyServer.stop();
                }
                catch (IllegalStateException ex) {
                    // ok to ignore
                }
            }

            TApplication application = getApplication();

            if (application != null && application instanceof ModuleOverrider && ((ModuleOverrider) (application)).getOverrideModules() != null) {
                (((ModuleOverrider) application)).getOverrideModules()
                                                 .stream()
                                                 .forEach(OverridableModule::close);
            }
        }
        finally {
            this.stopped = true;
        }
    }

    public Bootstrap<TConfiguration> getBootrapper(TApplication application, final long appPort) {
        return new Bootstrap<TConfiguration>(application) {
            @Override
            public void run(TConfiguration configuration, Environment environment) throws Exception {

                final DefaultServerFactory serverFactory = (DefaultServerFactory) configuration.getServerFactory();

                serverFactory.setJerseyRootPath(testHostConfiguration.getApplicationRoot());

                final HttpConnectorFactory appConnection = new HttpConnectorFactory();
                appConnection.setPort((int) appPort);

                final HttpConnectorFactory adminConnection = new HttpConnectorFactory();
                adminConnection.setPort((int) appPort + 1);

                serverFactory.setApplicationConnectors(Lists.newArrayList(appConnection));
                serverFactory.setAdminConnectors(Lists.newArrayList(adminConnection));

                environment.lifecycle().addServerLifecycleListener(server -> {
                    jettyServer = server;
                });

                super.run(configuration, environment);
            }
        };
    }
}
