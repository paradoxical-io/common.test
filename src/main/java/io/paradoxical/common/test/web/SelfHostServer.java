package io.paradoxical.common.test.web;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.paradoxical.common.test.guice.OverridableModule;
import io.paradoxical.common.test.web.runner.ServiceTestRunner;
import io.paradoxical.common.test.web.runner.ServiceTestRunnerConfig;
import io.paradoxical.common.test.web.runner.TestApplicationFactory;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Random;

public class SelfHostServer<
        TServiceConfiguration extends Configuration,
        TApplication extends Application<TServiceConfiguration>> implements AutoCloseable, Closeable {

    private static Random random = new Random();

    @Getter
    private final ImmutableList<OverridableModule> overridableModules;

    @Getter
    private ServiceTestRunner<TServiceConfiguration, TApplication> applicationServiceTestRunner;

    public SelfHostServer(OverridableModule... overridableModules) {
        this(Lists.newArrayList(overridableModules));
    }

    public SelfHostServer(List<OverridableModule> overridableModules) {
        this.overridableModules = ImmutableList.copyOf(overridableModules);
    }

    public void start(
            TestApplicationFactory<TApplication> applicationFactory,
            TServiceConfiguration configuration) throws Exception {
        start(applicationFactory, configuration, ServiceTestRunnerConfig.Default);
    }

    public void start(
            TestApplicationFactory<TApplication> applicationFactory,
            TServiceConfiguration configuration,
            ServiceTestRunnerConfig testRunnerConfig) throws Exception {

        if (applicationServiceTestRunner != null) {
            stop();
        }

        applicationServiceTestRunner =
                new ServiceTestRunner<>(
                        applicationFactory,
                        configuration,
                        getRandomPort());

        applicationServiceTestRunner.run(testRunnerConfig, overridableModules);
    }

    public void stop() throws Exception {
        applicationServiceTestRunner.close();
        applicationServiceTestRunner = null;
    }

    @Override
    public void close() throws IOException {
        try {
            stop();
        }
        catch (Exception e) {
            throw new IOException("Error stopping service", e);
        }
    }

    protected static int getRandomPort() {
        return random.nextInt(35000) + 15000;
    }

    public URI getServerUri() {
        if (applicationServiceTestRunner == null) {
            return null;
        }

        return applicationServiceTestRunner.getServerUri();
    }

    public URI getAdminUri() {
        if (applicationServiceTestRunner == null) {
            return null;
        }

        return applicationServiceTestRunner.getAdminUri();
    }
}