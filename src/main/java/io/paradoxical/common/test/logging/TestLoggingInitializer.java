package io.paradoxical.common.test.logging;

import ch.qos.logback.classic.Level;
import io.dropwizard.logging.BootstrapLogging;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class TestLoggingInitializer {
    public static void init() {

        final String defaultLogLevelEnv = System.getenv().getOrDefault("LOG_LEVEL", "OFF");

        BootstrapLogging.bootstrap(Level.toLevel(defaultLogLevelEnv));

        final String extraLogLevelEnv = System.getenv().getOrDefault("EXTRA_LOG_LEVEL", "OFF");

        init(Level.valueOf(defaultLogLevelEnv), Level.valueOf(extraLogLevelEnv));
    }

    public static void init(Level appLevel, Level extraLevel) {

        String[] disableLogging = new String[]{ "uk.co.jemos.podam.api.PodamFactoryImpl",
                                                "uk.co.jemos.podam.common.BeanValidationStrategy",
                                                "org.apache.cassandra.service.CassandraDaemon",
                                                "org.apache.cassandra.service.CacheService",
                                                "org.apache.cassandra.db.Memtable",
                                                "org.apache.cassandra.db.ColumnFamilyStore",
                                                "org.apache.cassandra.config.DatabaseDescriptor",
                                                "org.apache.cassandra.db.compaction.CompactionTask",
                                                "org.apache.cassandra.db.DefsTables",
                                                "org.apache.cassandra.service.MigrationManager",
                                                "org.apache.cassandra.config.YamlConfigurationLoader",
                                                "org.apache.cassandra.service.StorageService"
        };

        Arrays.stream(disableLogging).forEach(i -> {
            ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(i)).setLevel(extraLevel);
        });
    }
}
