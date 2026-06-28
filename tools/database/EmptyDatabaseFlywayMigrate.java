import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/** Guarded Flyway migration for a new, empty Supabase project. */
public final class EmptyDatabaseFlywayMigrate {
    private EmptyDatabaseFlywayMigrate() {
    }

    public static void main(String[] args) throws Exception {
        String host = requiredEnv("TARGET_DB_HOST");
        if (!host.equals(System.getenv("CONFIRM_TARGET_HOST")) || !host.endsWith(".supabase.com")) {
            throw new IllegalArgumentException("CONFIRM_TARGET_HOST must exactly match the Supabase target host");
        }
        String sslMode = env("TARGET_DB_SSLMODE", "require");
        if ("disable".equalsIgnoreCase(sslMode)) {
            throw new IllegalArgumentException("SSL cannot be disabled for the Supabase target");
        }

        String url = "jdbc:postgresql://" + host + ":" + env("TARGET_DB_PORT", "5432")
                + "/" + env("TARGET_DB_NAME", "postgres") + "?sslmode=" + sslMode;
        String user = requiredEnv("TARGET_DB_USER");
        String password = requiredEnv("TARGET_DB_PASSWORD");

        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {
            int applicationTables = scalar(statement, """
                    SELECT count(*) FROM pg_tables
                    WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
                    """);
            if (applicationTables != 0) {
                throw new IllegalStateException(
                        "Target public schema is not empty; found " + applicationTables + " tables");
            }
        }

        Flyway flyway = Flyway.configure()
                .dataSource(url, user, password)
                .locations("filesystem:src/main/resources/db/migration")
                .baselineOnMigrate(false)
                .validateMigrationNaming(true)
                .load();
        MigrateResult result = flyway.migrate();

        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {
            int tables = scalar(statement, """
                    SELECT count(*) FROM pg_tables
                    WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
                    """);
            int columns = scalar(statement, """
                    SELECT count(*) FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
                    """);
            int constraints = scalar(statement, """
                    SELECT count(*) FROM pg_constraint con
                    JOIN pg_class c ON c.oid = con.conrelid
                    JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'public' AND c.relname <> 'flyway_schema_history'
                    """);
            int indexes = scalar(statement, """
                    SELECT count(*) FROM pg_indexes
                    WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
                    """);
            if (tables != 23 || columns != 256 || constraints != 63 || indexes != 46) {
                throw new IllegalStateException("Migrated schema mismatch: tables=" + tables
                        + ", columns=" + columns + ", constraints=" + constraints + ", indexes=" + indexes);
            }
            System.out.println("Supabase schema verified: tables=" + tables + ", columns=" + columns
                    + ", constraints=" + constraints + ", indexes=" + indexes);
        }
        System.out.println("Flyway migration complete: initialVersion=" + result.initialSchemaVersion
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
