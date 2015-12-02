package io.paradoxical.common.test.web.runner;

import io.paradoxical.common.test.guice.OverridableModule;
import io.dropwizard.Application;
import io.dropwizard.Configuration;

import java.util.List;

@FunctionalInterface
public interface TestServiceFactory<TApplication extends Application<? extends  Configuration>> {

    static <TApplication extends Application<? extends  Configuration>>
        TestServiceFactory<TApplication> simpleReflectionFactory(Class<TApplication> applicationClass){
        return overrides -> {
            try {
                return applicationClass.newInstance();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    TApplication createService(List<OverridableModule> overridableModules);

}
