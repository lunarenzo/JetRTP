package lunatech.jetrtp.messaging;

import lunatech.jetrtp.database.DatabaseTestParams;
import lunatech.jetrtp.database.DatabaseTestUtils;
import lunatech.jetrtp.database.config.DatabaseConfig;
import lunatech.jetrtp.database.handler.DatabaseHandler;
import lunatech.jetrtp.messaging.config.MessagingConfig;
import lunatech.jetrtp.utility.DB;
import lunatech.jetrtp.utility.Messaging;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

@Tag("embeddedmessaging")
public abstract class AbstractEmbeddedMessagingTest extends AbstractMessagingTest {
    private static @TempDir Path TEMP_DIR; // Temporary directory for sqlite database
    private final DatabaseTestParams databaseTestConfig = DatabaseTestUtils.sqlite(DatabaseTestUtils.TablePrefix.EMPTY);

    AbstractEmbeddedMessagingTest(MessengerTestParams testConfig) {
        super(testConfig);
    }

    @BeforeAll
    @DisplayName("Initialize message broker")
    void beforeAllTests() {
        final DatabaseConfig databaseConfig = DatabaseConfig.builder()
            .withDatabaseType(databaseTestConfig.databaseType())
            .withPath(TEMP_DIR)
            .withTablePrefix(databaseTestConfig.tablePrefix())
            .build();
        Assertions.assertEquals(databaseTestConfig.requiredDatabaseType(), databaseConfig.getDatabaseType());

        DB.init(
            DatabaseHandler.builder()
                .withConfig(databaseConfig)
                .withLogger(logger)
                .withMigrate(true)
                .build()
        );
        DB.getHandler().doStartup();

        messagingConfig = MessagingConfig.builder()
            .withEnabled(true)
            .withPollingInterval(10)
            .withBroker(getTestConfig().type())
            .withAddresses("localhost:3306")
            .withUsername("")
            .withPassword("")
            .withSSL(false)
            .withRabbitMq("/")
            .build();

        Messaging.init(
            MessagingHandler.builder()
                .withConfig(messagingConfig)
                .withTesting(true)
                .withLogger(logger)
                .withName("Test")
                .withTaskAdapter(new MockTaskAdapter())
                .withReceiverAdapter(new MockReceiverAdapter())
                .build()
        );
        Messaging.getHandler().doStartup();
        Messaging.getHandler().scheduleTasks();
    }

    @AfterAll
    @Override
    void afterAllTests() {
        super.afterAllTests();
        DB.getHandler().doShutdown();
    }
}
