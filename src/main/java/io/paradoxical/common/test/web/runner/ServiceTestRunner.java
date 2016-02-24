package io.paradoxical.common.test.web.runner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.cli.ServerCommand;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.testing.ConfigOverride;
import io.paradoxical.common.test.guice.OverridableModule;
import lombok.NonNull;
import org.eclipse.jetty.server.Server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.UriBuilder;
import java.net.BindException;
import java.net.URI;
import java.util.List;

public class ServiceTestRunner<TConfiguration extends Configuration, TApplication extends Application<TConfiguration>>
        extends BaseServiceTestRunner<TConfiguration, TApplication> {

    private int port;

    public ServiceTestRunner(
            @NonNull @Nonnull @NotNull TestApplicationFactory<TApplication> applicationFactory,
            @NonNull @Nonnull @NotNull TConfiguration configuration,
            int port) {
        this(applicationFactory, port, configuration, null, ImmutableList.of());
    }

    public ServiceTestRunner(
            @NonNull @Nonnull @NotNull TestApplicationFactory<TApplication> applicationFactory,
            int port,
            @Nullable String configPath,
            ConfigOverride... configOverrides) {
        this(applicationFactory, port, null, configPath, ImmutableList.copyOf(configOverrides));
    }

    private ServiceTestRunner(
            @NonNull @Nonnull @NotNull TestApplicationFactory<TApplication> applicationFactory,
            int port,
            @Nullable TConfiguration configuration,
            @Nullable String configPath,
            @NonNull @Nonnull @NotNull ImmutableList<ConfigOverride> configOverrides) {

        super(applicationFactory, configuration, configPath, configOverrides);

        this.port = port;
    }

    public URI getServerUri() {
        return getServerUri("http");
    }

    public URI getServerUri(String scheme) {
        return getUri(scheme, getServerPort());
    }

    public URI getAdminUri() {
        return getAdminUri("http");
    }

    public URI getAdminUri(String scheme) {
        return getUri(scheme, getAdminPort());
    }

    private URI getUri(String scheme, int port) {
        return UriBuilder.fromUri("{scheme}://localhost:{port}/")
                         .buildFromMap(ImmutableMap.of(
                                 "scheme", scheme,
                                 "port", port));
    }

    public int getServerPort() {
        return port;
    }

    public int getAdminPort() {
        return getServerPort() + 1;
    }

    @Override
    protected void startIfRequired(final ImmutableList<OverridableModule> overridableModules) throws Exception {
        Exception cause = null;

        // retry if failures happen due to random port bindings
        for (int retry = 0; retry < 10; retry++) {
            try {
                super.startIfRequired(overridableModules);
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

        throw new RuntimeException(cause);
    }

    /**
     * Dynamically determine which startup command to choose. If no http return a fake startup command
     * This initializes the bootstrapper and any downstream environment stage (mostly for Guice's bootstrapper to get
     * access to the dropwizard configuration).
     */
    @Override
    protected EnvironmentCommand<TConfiguration> createStartupCommand(final TApplication application) {
        return new ServerCommand<>(application);
    }

    @Override
    protected Bootstrap<TConfiguration> createBootstrapSystem(TApplication application) {
        return new HttpBootstrapper<>(application, getTestHostConfiguration(), port);
    }

    @Override
    protected HttpBootstrapper<TConfiguration> getCurrentBootstrap() {
        return (HttpBootstrapper<TConfiguration>) super.getCurrentBootstrap();
    }

    @Override
    public void close() throws Exception {
        if (!isStarted()) {
            return;
        }

        final HttpBootstrapper<TConfiguration> currentBootstrap = getCurrentBootstrap();

        currentBootstrap.stopServer();

        super.close();
    }
}
