import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/** One-time guarded baseline for the existing non-empty local database. */
public final class ExistingDatabaseFlywayBaseline {
    private ExistingDatabaseFlywayBaseline() {
    }

    public static void main(String[] args) throws Exception {
        String database = env("DB_NAME", "shrimp_iot");
        if (!database.equals(System.getenv("CONFIRM_DATABASE"))) {
            throw new IllegalArgumentException("Set CONFIRM_DATABASE to the exact DB_NAME before baselining");
        }
        String url = "jdbc:postgresql://" + env("DB_HOST", "localhost") + ":"
                + env("DB_PORT", "5432") + "/" + database
                + "?sslmode=" + env("DB_SSLMODE", "disable");
        String user = requiredEnv("DB_USER");
        String password = requiredEnv("DB_PASSWORD");

        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {
            int tables = scalar(statement, """
                    SELECT count(*) FROM pg_tables
                    WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
                    """);
            if (tables != 23) {
                throw new IllegalStateException("Expected 23 application tables but found " + tables);
            }
        }

        Flyway flyway = Flyway.configure()
                .dataSource(url, user, password)
                .locations("filesystem:src/main/resources/db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .validateMigrationNaming(true)
                .load();
        MigrateResult result = flyway.migrate();
        System.out.println("Flyway baseline complete: database=" + database
                + ", initialVersion=" + result.initialSchemaVersion
                + ", targetVersion=" + result.targetSchemaVersion
                + ", migrationsExecuted=" + result.migrationsExecuted);
    }

    private static int scalar(Statement statement, String sql) throws Exception {
        try (ResultSet row = statement.executeQuery(sql)) {
            row.next();
            return row.getInt(1);
        }
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing environment: " + name);
        return value;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
