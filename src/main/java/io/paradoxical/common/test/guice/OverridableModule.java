package io.paradoxical.common.test.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public abstract class OverridableModule extends AbstractModule implements AutoCloseable {
    public abstract Class<? extends Module> getOverridesModule();

    public void close() {

    }
}
