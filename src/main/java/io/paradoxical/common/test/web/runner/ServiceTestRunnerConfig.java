package io.paradoxical.common.test.web.runner;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ServiceTestRunnerConfig {
    private static final String defaultFormat = "%d [%p] %marker [%mdc{corrId}] %logger %m%n";
    private static final String defaultApplicationRoot = "/api/*";

    private String logFormat;

    public ServiceTestRunnerConfig(String logFormat, String applicationRoot) {
        this.logFormat = logFormat == null ? defaultFormat : logFormat;
        this.applicationRoot = applicationRoot == null ? defaultApplicationRoot : applicationRoot;
    }

    private String applicationRoot = defaultApplicationRoot;

    public static ServiceTestRunnerConfig Default = new ServiceTestRunnerConfig(defaultFormat, defaultApplicationRoot);
}
