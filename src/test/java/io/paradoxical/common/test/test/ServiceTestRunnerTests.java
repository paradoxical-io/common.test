package io.paradoxical.common.test.test;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.paradoxical.common.test.web.runner.ServiceTestRunner;
import io.paradoxical.common.test.web.runner.TestApplicationFactory;
import lombok.Cleanup;
import lombok.Getter;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.net.URI;
import java.net.URISyntaxException;

import static org.fest.assertions.Assertions.assertThat;

public class ServiceTestRunnerTests extends TestBase {

    @Test
    public void when_getting_the_service_uri_it_should_respect_the_scheme() throws URISyntaxException {
        final ServiceTestRunner<AppConfig, App> serviceTestRunner =
                new ServiceTestRunner<>(TestApplicationFactory.simpleReflectionFactory(App.class),
                                        new AppConfig(),
                                        8080);

        final URI httpServerUri = serviceTestRunner.getServerUri();
        assertThat(httpServerUri).isEqualTo(new URI("http://localhost:8080/"));

        final URI httpsServerUri = serviceTestRunner.getServerUri("https");
        assertThat(httpsServerUri).isEqualTo(new URI("https://localhost:8080/"));


        final URI httpAdminUri = serviceTestRunner.getAdminUri();
        assertThat(httpAdminUri).isEqualTo(new URI("http://localhost:8081/"));

        final URI httpsAdminUri = serviceTestRunner.getAdminUri("https");
        assertThat(httpsAdminUri).isEqualTo(new URI("https://localhost:8081/"));
    }

    @Test
    public void when_starting_a_service_it_should_report_started() throws Exception {
        @Cleanup final ServiceTestRunner<AppConfig, App> serviceTestRunner =
                new ServiceTestRunner<>(TestApplicationFactory.simpleReflectionFactory(App.class),
                                        new AppConfig(),
                                        8080);

        serviceTestRunner.run();

        assertThat(serviceTestRunner.isStarted()).isTrue();
    }


    public static class AppConfig extends Configuration {
    }

    @Path("/")
    static class AppResource {
        @GET
        public String resouce() {
            return "Hello";
        }
    }

    public static class App extends Application<AppConfig> {

        @Override
        public void run(final AppConfig configuration, final Environment environment) throws Exception {
            environment.jersey().register(AppResource.class);
        }
    }
}
