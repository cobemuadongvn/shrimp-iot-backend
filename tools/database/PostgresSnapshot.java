import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Creates a local, Git-ignored PostgreSQL snapshot without requiring pg_dump.
 * Credentials are read only from DB_HOST, DB_PORT, DB_NAME, DB_USER and DB_PASSWORD.
 */
public final class PostgresSnapshot {
    private static final String SCHEMA = "public";

    private PostgresSnapshot() {
    }

    public static void main(String[] args) throws Exception {
        String host = env("DB_HOST", "localhost");
        String port = env("DB_PORT", "5432");
        String database = env("DB_NAME", "shrimp_iot");
        String user = requiredEnv("DB_USER");
        String password = requiredEnv("DB_PASSWORD");

        Path root = args.length > 0 ? Path.of(args[0]) : Path.of("backups", "postgres");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path snapshot = root.resolve(database + "-" + timestamp).toAbsolutePath().normalize();
        Path dataDir = snapshot.resolve("data");
        Files.createDirectories(dataDir);

        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY");
            }

            List<String> tables = listTables(connection);
            writeQueryCsv(connection, snapshot.resolve("columns.csv"), """
                    SELECT table_name, ordinal_position, column_name, data_type, udt_name,
                           is_nullable, column_default, character_maximum_length,
                           numeric_precision, numeric_scale, is_identity, identity_generation
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                    ORDER BY table_name, ordinal_position
                    """);
            writeQueryCsv(connection, snapshot.resolve("constraints.csv"), """
                    SELECT c.relname AS table_name, con.conname AS constraint_name,
                           con.contype AS constraint_type, pg_get_constraintdef(con.oid, true) AS definition
                    FROM pg_constraint con
                    JOIN pg_class c ON c.oid = con.conrelid
                    JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'public'
                    ORDER BY c.relname, con.conname
                    """);
            writeQueryCsv(connection, snapshot.resolve("indexes.csv"), """
                    SELECT tablename AS table_name, indexname AS index_name, indexdef AS definition
                    FROM pg_indexes
                    WHERE schemaname = 'public'
                    ORDER BY tablename, indexname
                    """);
            writeQueryCsv(connection, snapshot.resolve("sequences.csv"), """
                    SELECT sequencename AS sequence_name, start_value, min_value, max_value,
                           increment_by, cycle, cache_size, last_value
                    FROM pg_sequences
                    WHERE schemaname = 'public'
                    ORDER BY sequencename
                    """);

            CopyManager copyManager = connection.unwrap(PGConnection.class).getCopyAPI();
            List<TableSnapshot> tableSnapshots = new ArrayList<>();
            for (String table : tables) {
                long rows = countRows(connection, table);
                Path output = dataDir.resolve(table + ".csv");
                String copySql = "COPY " + qualified(table)
                        + " TO STDOUT WITH (FORMAT CSV, HEADER true, ENCODING 'UTF8')";
                try (OutputStream stream = Files.newOutputStream(output)) {
                    copyManager.copyOut(copySql, stream);
                }
                tableSnapshots.add(new TableSnapshot(table, rows, sha256(output)));
            }

            try (BufferedWriter writer = Files.newBufferedWriter(snapshot.resolve("manifest.txt"), StandardCharsets.UTF_8)) {
                writer.write("created_at=" + LocalDateTime.now() + System.lineSeparator());
                writer.write("database=" + database + System.lineSeparator());
                writer.write("schema=" + SCHEMA + System.lineSeparator());
                writer.write("table_count=" + tables.size() + System.lineSeparator());
                writer.write(System.lineSeparator());
                writer.write("table,row_count,sha256" + System.lineSeparator());
                for (TableSnapshot item : tableSnapshots) {
                    writer.write(item.table() + "," + item.rows() + "," + item.sha256() + System.lineSeparator());
                }
            }

            connection.rollback();
            System.out.println("Snapshot created: " + snapshot);
            System.out.println("Tables exported: " + tables.size());
        }
    }

    private static List<String> listTables(Connection connection) throws Exception {
        List<String> tables = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT tablename
                FROM pg_tables
                WHERE schemaname = ?
                  AND tablename <> 'flyway_schema_history'
                ORDER BY tablename
                """)) {
            statement.setString(1, SCHEMA);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    tables.add(rows.getString(1));
                }
            }
        }
        return tables;
    }

    private static long countRows(Connection connection, String table) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery("SELECT count(*) FROM " + qualified(table))) {
            row.next();
            return row.getLong(1);
        }
    }

    private static void writeQueryCsv(Connection connection, Path output, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql);
             BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            ResultSetMetaData metadata = rows.getMetaData();
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                if (i > 1) writer.write(',');
                writer.write(csv(metadata.getColumnLabel(i)));
            }
            writer.newLine();
            while (rows.next()) {
                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    if (i > 1) writer.write(',');
                    writer.write(csv(rows.getString(i)));
                }
                writer.newLine();
            }
        }
    }

    private static String qualified(String table) {
        return quote(SCHEMA) + "." + quote(table);
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required environment variable: " + name);
        }
        return value;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private record TableSnapshot(String table, long rows, String sha256) {
    }
}
