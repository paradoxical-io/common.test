package io.paradoxical.common.test.web.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;

import javax.validation.Validator;
import java.io.File;
import java.io.IOException;

public abstract class StaticConfigurationFactory<T> extends ConfigurationFactory<T> {
    /**
     * Creates a new configuration factory for the given class.
     *
     * @param klass          the configuration class
     * @param validator      the validator to use
     * @param objectMapper   the Jackson {@link ObjectMapper} to use
     * @param propertyPrefix the system property name prefix used by overrides
     */
    public StaticConfigurationFactory(
            final Class<T> klass,
            final Validator validator,
            final ObjectMapper objectMapper,
            final String propertyPrefix) {
        super(klass, validator, objectMapper, propertyPrefix);
    }

    public StaticConfigurationFactory() {
        super(null, null, new ObjectMapper(), "");
    }

    @Override public T build(final ConfigurationSourceProvider provider, final String path) throws IOException, ConfigurationException {
        return provideConfig();
    }

    @Override public T build(final File file) throws IOException, ConfigurationException {
        return provideConfig();
    }

    @Override public T build() throws IOException, ConfigurationException {
        return provideConfig();
    }

    public abstract T provideConfig();
}
