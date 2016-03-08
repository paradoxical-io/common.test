package io.paradoxical.common.test.web.runner;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ServiceTestRunnerConfig {
    private static final String defaultFormat = "%d [%p] %marker [%mdc{corrId}] %logger %m%n";
    private static final String defaultApplicationRoot = "/api/*";
    public static final ServiceTestRunnerConfig Default = new ServiceTestRunnerConfig(defaultFormat, defaultApplicationRoot);

    private String logFormat;
    private String applicationRoot;

    public ServiceTestRunnerConfig(String logFormat, String applicationRoot) {
        this.logFormat = logFormat == null ? defaultFormat : logFormat;
        this.applicationRoot = applicationRoot == null ? defaultApplicationRoot : applicationRoot;
    }
}
