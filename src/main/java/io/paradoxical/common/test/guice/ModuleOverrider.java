package io.paradoxical.common.test.guice;

import java.util.List;

public interface ModuleOverrider {
    void overrideModulesWith(List<OverridableModule> modules);

    List<OverridableModule> getOverrideModules();
}
