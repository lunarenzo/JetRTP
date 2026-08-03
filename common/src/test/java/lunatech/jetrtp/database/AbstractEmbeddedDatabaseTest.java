package lunatech.jetrtp.database;

import lunatech.jetrtp.database.config.DatabaseConfig;
import lunatech.jetrtp.database.handler.DatabaseHandler;
import lunatech.jetrtp.utility.DB;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

@Tag("embeddeddatabase")
public abstract class AbstractEmbeddedDatabaseTest extends AbstractDatabaseTest {
    private static @TempDir Path TEMP_DIR;

    AbstractEmbeddedDatabaseTest(DatabaseTestParams testConfig) {
        super(testConfig);
    }

    @BeforeAll
    @DisplayName("Initialize connection pool")
    void beforeAllTests() {
        databaseConfig = DatabaseConfig.builder()
            .withDatabaseType(getTestConfig().databaseType())
            .withPath(TEMP_DIR)
            .withTablePrefix(getTestConfig().tablePrefix())
            .build();
        Assertions.assertEquals(getTestConfig().requiredDatabaseType(), databaseConfig.getDatabaseType());

        DB.init(
            DatabaseHandler.builder()
                .withConfig(databaseConfig)
                .withLogger(logger)
                .build()
        );
        DB.getHandler().doStartup();
    }
}
