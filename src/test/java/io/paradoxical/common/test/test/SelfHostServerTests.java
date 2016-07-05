package io.paradoxical.common.test.test;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.paradoxical.common.test.web.SelfHostServer;
import io.paradoxical.common.test.web.runner.ServiceTestRunnerConfig;
import lombok.Cleanup;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import static org.assertj.core.api.Assertions.assertThat;


public class SelfHostServerTests extends TestBase {
    @Test
    public void when_starting_a_service_it_should_report_started() throws Exception {
        @Cleanup final SelfHostServer<AppConfig, App> appConfigAppSelfHostServer = new SelfHostServer<>();

        appConfigAppSelfHostServer.start(overrideModules -> new App(), new AppConfig(),
                                         ServiceTestRunnerConfig.Default.toBuilder()
                                                                        .applicationRoot("/")
                                                                        .build());

        assertThat(appConfigAppSelfHostServer.getApplicationServiceTestRunner().isStarted()).isTrue();
        Client client = ClientBuilder.newClient();

        final String entity = client.target(appConfigAppSelfHostServer.getServerUri())
                                    .path("/")
                                    .request()
                                    .get()
                                    .readEntity(String.class);

        assertThat(entity).isEqualTo("Hello");

    }

    public static class AppConfig extends Configuration {
    }

    @Path("/")
    public static class AppResource {
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