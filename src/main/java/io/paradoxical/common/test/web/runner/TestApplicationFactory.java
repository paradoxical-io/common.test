package io.paradoxical.common.test.web.runner;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.paradoxical.common.test.guice.OverridableModule;

import java.util.List;

@FunctionalInterface
public interface TestApplicationFactory<TApplication extends Application<? extends Configuration>> {

    static <TApplication extends Application<? extends Configuration>>
    TestApplicationFactory<TApplication> simpleReflectionFactory(Class<TApplication> applicationClass) {
        return new ReflectionApplicationFactory<>(applicationClass);
    }

    TApplication createService(List<OverridableModule> overrideModules);
}
