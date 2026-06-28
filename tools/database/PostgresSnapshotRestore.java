import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Restores a verified local snapshot into an empty Supabase schema. */
public final class PostgresSnapshotRestore {
    private static final List<String> IMPORT_ORDER = List.of(
            "ponds", "users", "devices",
            "alerts", "audit_logs", "auth_tokens", "chat_sessions", "chat_messages",
            "control_scenarios", "device_commands", "device_latest_states",
            "device_operation_configs", "device_provisioning", "device_relays", "device_sensors",
            "measurement_cycles", "notification_logs", "relay_states", "salinity_correction_cycles",
            "sensor_calibrations", "sensor_readings", "threshold_configs", "user_pond_access"
    );

    private PostgresSnapshotRestore() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: PostgresSnapshotRestore <snapshot-directory>");
        }
        Path snapshot = Path.of(args[0]).toAbsolutePath().normalize();
        Path data = snapshot.resolve("data");
        Path manifest = snapshot.resolve("manifest.txt");
        if (!Files.isDirectory(data) || !Files.isRegularFile(manifest)) {
            throw new IllegalArgumentException("Invalid snapshot directory: " + snapshot);
        }

        Map<String, SnapshotTable> expected = readManifest(manifest);
        if (!expected.keySet().equals(new java.util.LinkedHashSet<>(IMPORT_ORDER))) {
            throw new IllegalStateException("Snapshot table set does not match the 23-table import contract");
        }
        for (SnapshotTable item : expected.values()) {
            Path file = data.resolve(item.table() + ".csv");
            if (!Files.isRegularFile(file) || !item.sha256().equals(sha256(file))) {
                throw new IllegalStateException("Snapshot checksum failed for table: " + item.table());
            }
        }

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

        try (Connection connection = DriverManager.getConnection(
                url, requiredEnv("TARGET_DB_USER"), requiredEnv("TARGET_DB_PASSWORD"))) {
            connection.setAutoCommit(false);
            try {
                ensureTargetIsEmpty(connection);
                CopyManager copy = connection.unwrap(PGConnection.class).getCopyAPI();
                long totalRows = 0;
                for (String table : IMPORT_ORDER) {
                    SnapshotTable item = expected.get(table);
                    try (InputStream input = Files.newInputStream(data.resolve(table + ".csv"))) {
                        long copied = copy.copyIn("COPY public." + q(table)
                                + " FROM STDIN WITH (FORMAT CSV, HEADER true, ENCODING 'UTF8')", input);
                        if (copied != item.rows()) {
                            throw new IllegalStateException("Row count while importing " + table
                                    + ": expected=" + item.rows() + ", copied=" + copied);
                        }
                        totalRows += copied;
                    }
                }
                resetIdentitySequences(connection);
                verifyCounts(connection, expected);
                connection.commit();
                System.out.println("Snapshot restore complete: tables=" + expected.size()
                        + ", rows=" + totalRows);
            } catch (Exception error) {
                connection.rollback();
                throw error;
            }
        }
    }

    private static Map<String, SnapshotTable> readManifest(Path manifest) throws Exception {
        Map<String, SnapshotTable> result = new LinkedHashMap<>();
        boolean rowsStarted = false;
        for (String line : Files.readAllLines(manifest)) {
            if ("table,row_count,sha256".equals(line.trim())) {
                rowsStarted = true;
                continue;
            }
            if (!rowsStarted || line.isBlank()) continue;
            String[] parts = line.split(",", 3);
            if (parts.length != 3) throw new IllegalStateException("Invalid manifest row: " + line);
            SnapshotTable item = new SnapshotTable(parts[0], Long.parseLong(parts[1]), parts[2]);
            result.put(item.table(), item);
        }
        return result;
    }

    private static void ensureTargetIsEmpty(Connection connection) throws Exception {
        List<String> nonEmpty = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            for (String table : IMPORT_ORDER) {
                try (ResultSet row = statement.executeQuery("SELECT EXISTS (SELECT 1 FROM public."
                        + q(table) + " LIMIT 1)")) {
                    row.next();
                    if (row.getBoolean(1)) nonEmpty.add(table);
                }
            }
        }
        if (!nonEmpty.isEmpty()) {
            throw new IllegalStateException("Target tables must be empty before restore: " + nonEmpty);
        }
    }

    private static void resetIdentitySequences(Connection connection) throws Exception {
        for (String table : IMPORT_ORDER) {
            if (!hasIdColumn(connection, table)) continue;
            String sequence = null;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT pg_get_serial_sequence(?, 'id')")) {
                statement.setString(1, "public." + table);
                try (ResultSet row = statement.executeQuery()) {
                    row.next();
                    sequence = row.getString(1);
                }
            }
            if (sequence == null) continue;
            String setValue = "SELECT setval(?::regclass, "
                    + "COALESCE((SELECT max(id) FROM public." + q(table) + "), 1), "
                    + "EXISTS (SELECT 1 FROM public." + q(table) + "))";
            try (PreparedStatement statement = connection.prepareStatement(setValue)) {
                statement.setString(1, sequence);
                statement.executeQuery().close();
            }
        }
    }

    private static boolean hasIdColumn(Connection connection, String table) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = ? AND column_name = 'id'
                )
                """)) {
            statement.setString(1, table);
            try (ResultSet row = statement.executeQuery()) {
                row.next();
                return row.getBoolean(1);
            }
        }
    }

    private static void verifyCounts(Connection connection, Map<String, SnapshotTable> expected) throws Exception {
        try (Statement statement = connection.createStatement()) {
            for (SnapshotTable item : expected.values()) {
                try (ResultSet row = statement.executeQuery(
                        "SELECT count(*) FROM public." + q(item.table()))) {
                    row.next();
                    long actual = row.getLong(1);
                    if (actual != item.rows()) {
                        throw new IllegalStateException("Post-import count failed for " + item.table()
                                + ": expected=" + item.rows() + ", actual=" + actual);
                    }
                }
            }
        }
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String q(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
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

    private record SnapshotTable(String table, long rows, String sha256) {
    }
}
