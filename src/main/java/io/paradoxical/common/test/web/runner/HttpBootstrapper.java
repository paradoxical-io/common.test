package io.paradoxical.common.test.web.runner;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import org.eclipse.jetty.server.Server;

import java.util.List;

public class HttpBootstrapper<TConfiguration extends Configuration> extends Bootstrap<TConfiguration> {

    private final ServiceTestRunnerConfig serviceTestRunnerConfig;

    private final int httpPort;

    @Getter
    private Server jettyServer;

    /**
     * Creates a new {@link Bootstrap} for the given application.
     *
     * @param application a Dropwizard {@link Application}
     * @param serviceTestRunnerConfig
     * @param httpPort
     */
    public HttpBootstrapper(
            final Application<TConfiguration> application,
            final ServiceTestRunnerConfig serviceTestRunnerConfig,
            final int httpPort) {
        super(application);
        this.serviceTestRunnerConfig = serviceTestRunnerConfig;
        this.httpPort = httpPort;
    }

    @Override
    public void run(final TConfiguration configuration, final Environment environment) throws Exception {
        final ServerFactory serverFactory = configuration.getServerFactory();

        if (serverFactory instanceof DefaultServerFactory) {

            final DefaultServerFactory defaultServerFactory = (DefaultServerFactory) serverFactory;

            defaultServerFactory.setJerseyRootPath(serviceTestRunnerConfig.getApplicationRoot());

            configureServerPort(defaultServerFactory, httpPort);
        }
        else if (serverFactory instanceof SimpleServerFactory) {
            final SimpleServerFactory simpleServerFactory = (SimpleServerFactory) serverFactory;
            final ConnectorFactory connectorFactory = simpleServerFactory.getConnector();
            simpleServerFactory.setJerseyRootPath(serviceTestRunnerConfig.getApplicationRoot());
            configureConnectorPort(connectorFactory, httpPort);
        }
        else {
            throw new UnsupportedOperationException("The provided server factory is not supported for testing.");
        }

        environment.lifecycle()
                   .addServerLifecycleListener(server -> jettyServer = server);

        super.run(configuration, environment);
    }

    public void stopServer() throws Exception {
        if (jettyServer != null && jettyServer.isRunning()) {
            try {
                jettyServer.stop();
            }
            catch (IllegalStateException ex) {
                // ok to ignore
            }
        }
    }

    private static void configureServerPort(final DefaultServerFactory defaultServerFactory, final int port) {
        final List<ConnectorFactory> applicationConnectors = defaultServerFactory.getApplicationConnectors();
        if (applicationConnectors.size() == 1) {
            final ConnectorFactory connectorFactory = applicationConnectors.get(0);
            configureConnectorPort(connectorFactory, port);
        }

        final List<ConnectorFactory> adminConnectors = defaultServerFactory.getAdminConnectors();

        if (adminConnectors.size() == 1) {
            final ConnectorFactory connectorFactory = adminConnectors.get(0);
            configureConnectorPort(connectorFactory, port + 1);
        }
    }

    private static void configureConnectorPort(final ConnectorFactory connectorFactory, final int port) {
        if (!(connectorFactory instanceof HttpConnectorFactory)) {
            throw new UnsupportedOperationException("Unsupported connector factory. Expected either Http or Https.");
        }

        final HttpConnectorFactory httpConnectorFactory = (HttpConnectorFactory) connectorFactory;

        httpConnectorFactory.setPort(port);
    }
}
