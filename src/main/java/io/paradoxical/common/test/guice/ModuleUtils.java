package io.paradoxical.common.test.guice;

import com.google.common.collect.Lists;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class ModuleUtils {
    public static List<Module> mergeModules(List<Module> modules, OverridableModule... overrides) {
        return mergeModules(modules, Arrays.asList(overrides));
    }

    public static List<Module> mergeModules(List<Module> modules, List<OverridableModule> overrides) {
        final List<OverridableModule> additiveModules =
                overrides != null ?
                overrides.stream().filter(i -> i.getOverridesModule() == null).collect(toList()) :
                Lists.newArrayList();

        final List<Module> overridenModules =
                modules.stream()
                       .map(defaultModule -> {
                           if (overrides == null) {
                               return defaultModule;
                           }

                           final Optional<OverridableModule> first =
                                   overrides.stream()
                                            .filter(overridingModule ->
                                                            overridingModule.getOverridesModule() != null &&
                                                            defaultModule.getClass().isAssignableFrom(overridingModule.getOverridesModule()))
                                            .findFirst();

                           if (first.isPresent()) {
                               return Modules.override(defaultModule).with(first.get());
                           }

                           return defaultModule;
                       })
                       .collect(toList());
        if (additiveModules.size() > 0) {
            overridenModules.addAll(additiveModules);
        }

        return overridenModules;
    }
}
