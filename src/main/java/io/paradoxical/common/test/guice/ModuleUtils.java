package io.paradoxical.common.test.guice;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class ModuleUtils {
    public static ImmutableList<Module> mergeModules(Collection<Module> modules, OverridableModule... overrides) {
        return mergeModules(modules, ImmutableList.copyOf(overrides));
    }

    public static ImmutableList<Module> mergeModules(Collection<Module> modules, List<OverridableModule> overrides) {
        final ImmutableList<OverridableModule> additiveModules =
                Optional.ofNullable(overrides)
                        .map(overrideList -> overrideList.stream()
                                                         .filter(i -> i.getOverridesModule() == null)
                                                         .collect(toList()))
                        .map(ImmutableList::copyOf)
                        .orElseGet(ImmutableList::of);


        final List<Module> overriddenModules =
                modules.stream()
                       .map(defaultModule -> {
                           if (overrides == null) {
                               return defaultModule;
                           }

                           final List<OverridableModule> overrideModules =
                                   overrides.stream()
                                            .filter(overridingModule ->
                                                            overridingModule.getOverridesModule() != null &&
                                                            defaultModule.getClass()
                                                                         .isAssignableFrom(overridingModule.getOverridesModule()))
                                            .collect(toList());

                           if (!overrideModules.isEmpty()) {
                               return Modules.override(defaultModule)
                                             .with(overrideModules);
                           }

                           return defaultModule;
                       })
                       .collect(toList());

        if (additiveModules.size() > 0) {
            overriddenModules.addAll(additiveModules);
        }

        return ImmutableList.copyOf(overriddenModules);
    }
}
