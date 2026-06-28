import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

/** Executes V1 in a disposable schema and always removes it afterwards. */
public final class PostgresMigrationVerifier {
    private PostgresMigrationVerifier() {
    }

    public static void main(String[] args) throws Exception {
        Path migration = Path.of("src", "main", "resources", "db", "migration", "V1__baseline_schema.sql");
        String schema = "flyway_verify_" + System.currentTimeMillis();
        String quotedSchema = "\"" + schema + "\"";
        String sql = Files.readString(migration)
                .replace("public.", quotedSchema + ".");

        String url = "jdbc:postgresql://" + env("DB_HOST", "localhost") + ":"
                + env("DB_PORT", "5432") + "/" + env("DB_NAME", "shrimp_iot")
                + "?sslmode=" + env("DB_SSLMODE", "disable");
        try (Connection connection = DriverManager.getConnection(
                url, requiredEnv("DB_USER"), requiredEnv("DB_PASSWORD"));
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                statement.execute("CREATE SCHEMA " + quotedSchema);
                statement.execute("SET LOCAL search_path TO " + quotedSchema);
                for (String command : sql.split(";\\s*(?:\\r?\\n|$)")) {
                    String trimmed = command.trim();
                    if (!trimmed.isEmpty()) statement.execute(trimmed);
                }

                int tables = scalar(statement, "SELECT count(*) FROM pg_tables WHERE schemaname = '" + schema + "'");
                int columns = scalar(statement, """
                        SELECT count(*) FROM information_schema.columns
                        WHERE table_schema = '%s'
                        """.formatted(schema));
                int constraints = scalar(statement, """
                        SELECT count(*) FROM pg_constraint con
                        JOIN pg_class c ON c.oid = con.conrelid
                        JOIN pg_namespace n ON n.oid = c.relnamespace
                        WHERE n.nspname = '%s'
                        """.formatted(schema));
                int indexes = scalar(statement, "SELECT count(*) FROM pg_indexes WHERE schemaname = '" + schema + "'");
                if (tables != 23 || columns != 256 || constraints != 63 || indexes != 46) {
                    throw new IllegalStateException("Schema mismatch: tables=" + tables + ", columns=" + columns
                            + ", constraints=" + constraints + ", indexes=" + indexes);
                }
                System.out.printf(Locale.ROOT,
                        "Migration verified: tables=%d, columns=%d, constraints=%d, indexes=%d%n",
                        tables, columns, constraints, indexes);
            } finally {
                connection.rollback();
            }
        }
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
