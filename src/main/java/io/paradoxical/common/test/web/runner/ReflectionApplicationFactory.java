package io.paradoxical.common.test.web.runner;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.paradoxical.common.test.guice.OverridableModule;

import java.util.List;

public class ReflectionApplicationFactory<TApplication extends Application<? extends Configuration>>
        implements TestApplicationFactory<TApplication> {

    private final Class<TApplication> applicationClass;

    public ReflectionApplicationFactory(final Class<TApplication> applicationClass) {
        this.applicationClass = applicationClass;
    }

    @Override
    public TApplication createService(final List<OverridableModule> overrideModules) {
        try {
            return applicationClass.newInstance();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
