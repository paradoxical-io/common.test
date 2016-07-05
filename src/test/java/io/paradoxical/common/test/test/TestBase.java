package io.paradoxical.common.test.test;

import io.dropwizard.logging.BootstrapLogging;

public class TestBase {


    public TestBase() {
        BootstrapLogging.bootstrap();
    }
}
