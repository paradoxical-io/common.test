package io.paradoxical.common.test.web.runner;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.logging.ConsoleAppenderFactory;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.POJOConfigurationFactory;
import io.paradoxical.common.test.guice.OverridableModule;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.sourceforge.argparse4j.inf.Namespace;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.List;

public class BaseServiceTestRunner<TConfiguration extends Configuration, TApplication extends Application<TConfiguration>> implements AutoCloseable {

    @Getter(AccessLevel.PROTECTED)
    private final TestApplicationFactory<TApplication> applicationFactory;

    @Getter(AccessLevel.PROTECTED)
    private final String configPath;

    @Getter(AccessLevel.PROTECTED)
    private final TConfiguration configuration;


    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private ServiceTestRunnerConfig testHostConfiguration;

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private TApplication application;

    @Getter
    private boolean started = false;

    @Getter(AccessLevel.PROTECTED)
    private Bootstrap<TConfiguration> currentBootstrap;

    public BaseServiceTestRunner(
            @NonNull @Nonnull @NotNull TestApplicationFactory<TApplication> applicationFactory,
            @NonNull @Nonnull @NotNull TConfiguration configuration) {
        this(applicationFactory, configuration, null, ImmutableList.of());
    }

    protected BaseServiceTestRunner(
            @NonNull @Nonnull @NotNull TestApplicationFactory<TApplication> applicationFactory,
            @Nullable TConfiguration configuration,
            @Nullable String configPath,
            @NonNull @Nonnull @NotNull ImmutableList<ConfigOverride> configOverrides) {

        this.applicationFactory = applicationFactory;
        this.configPath = configPath;
        this.configuration = configuration;

        configOverrides.forEach(ConfigOverride::addToSystemProperties);
    }

    public BaseServiceTestRunner(
            @NonNull @Nonnull @NotNull TestApplicationFactory<TApplication> applicationFactory,
            @Nullable String configPath,
            ConfigOverride... configOverrides) {
        this(applicationFactory, null, configPath, ImmutableList.copyOf(configOverrides));
    }

    public BaseServiceTestRunner<TConfiguration, TApplication> run() throws Exception {
        return run(ImmutableList.of());
    }

    public BaseServiceTestRunner<TConfiguration, TApplication> run(List<OverridableModule> overridableModules) throws Exception {
        return run(ServiceTestRunnerConfig.Default, overridableModules);
    }

    public BaseServiceTestRunner<TConfiguration, TApplication> run(ServiceTestRunnerConfig config, List<OverridableModule> overridableModules) throws Exception {
        this.testHostConfiguration = config;

        startIfRequired(ImmutableList.copyOf(overridableModules));

        return this;
    }

    protected void startIfRequired(final ImmutableList<OverridableModule> overridableModules) throws Exception {
        if (started) {
            return;
        }

        currentBootstrap = internalInit(overridableModules);

        final EnvironmentCommand<TConfiguration> command = createStartupCommand(application);

        ImmutableMap.Builder<String, Object> file = ImmutableMap.builder();

        if (!Strings.isNullOrEmpty(configPath)) {
            file.put("file", configPath);
        }

        final Namespace namespace = new Namespace(file.build());

        command.run(currentBootstrap, namespace);

        started = true;
    }

    /**
     * Customize the dropwizard bootstrapping code to provide a custom logger, and return the provided config
     * This way we aren't bound to files to pass in configs and can override things in code
     *
     * @param overridableModules The list of modules to use
     * @return a bootstrap system for self-hosting
     */
    protected Bootstrap<TConfiguration> internalInit(final ImmutableList<OverridableModule> overridableModules) {

        application = applicationFactory.createService(overridableModules);

        final Bootstrap<TConfiguration> bootstrap = createBootstrapSystem(application);

        if (configuration != null) {

            setupLogging();

            final POJOConfigurationFactory<TConfiguration> pojoConfigurationFactory =
                    new POJOConfigurationFactory<>(configuration);

            bootstrap.setConfigurationFactoryFactory((klass, validator, objectMapper, propertyPrefix) -> pojoConfigurationFactory);
        }

        application.initialize(bootstrap);

        return bootstrap;
    }

    private void setupLogging() {
        final DefaultLoggingFactory loggingFactory = ((DefaultLoggingFactory) configuration.getLoggingFactory());

        if (loggingFactory == null) {
            return;
        }

        final ConsoleAppenderFactory<ILoggingEvent> testAppender = new ConsoleAppenderFactory<>();

        testAppender.setLogFormat(testHostConfiguration.getLogFormat());

        loggingFactory.setAppenders(ImmutableList.of(testAppender));
    }

    protected EnvironmentCommand<TConfiguration> createStartupCommand(final TApplication application) {
        return new EnvironmentCommand<TConfiguration>(application, "non-running", "test") {
            @Override
            protected void run(final Environment environment, final Namespace namespace, final TConfiguration configuration) throws Exception {
            }
        };
    }

    protected Bootstrap<TConfiguration> createBootstrapSystem(TApplication application) {
        return new Bootstrap<>(application);
    }

    public BaseServiceTestRunner<TConfiguration, TApplication> run(ServiceTestRunnerConfig config) throws Exception {
        return run(config, ImmutableList.of());
    }

    @Override
    public void close() throws Exception {
        if (!isStarted()) {
            return;
        }

        TApplication application = getApplication();

        if (application instanceof AutoCloseable) {
            ((AutoCloseable) application).close();
        }

        started = false;
    }
}
