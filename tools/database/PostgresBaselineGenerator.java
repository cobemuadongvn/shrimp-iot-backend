import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Prints a deterministic Flyway V1 schema from the live public PostgreSQL schema. */
public final class PostgresBaselineGenerator {
    private PostgresBaselineGenerator() {
    }

    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://" + env("DB_HOST", "localhost") + ":"
                + env("DB_PORT", "5432") + "/" + env("DB_NAME", "shrimp_iot");
        try (Connection connection = DriverManager.getConnection(
                url, requiredEnv("DB_USER"), requiredEnv("DB_PASSWORD"))) {
            connection.setReadOnly(true);
            StringBuilder sql = new StringBuilder();
            sql.append("-- Baseline generated from the verified local PostgreSQL schema.\n")
                    .append("-- Existing databases are baselined at V1; empty databases execute this file.\n\n");

            List<String> tables = listTables(connection);
            for (String table : tables) {
                appendTable(connection, sql, table);
            }
            appendConstraints(connection, sql, false);
            appendConstraints(connection, sql, true);
            appendIndexes(connection, sql);
            System.out.print(sql);
        }
    }

    private static void appendSequences(Connection connection, StringBuilder sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     SELECT c.relname, format_type(s.seqtypid, NULL), s.seqstart, s.seqincrement,
                            s.seqmin, s.seqmax, s.seqcache, s.seqcycle
                     FROM pg_sequence s
                     JOIN pg_class c ON c.oid = s.seqrelid
                     JOIN pg_namespace n ON n.oid = c.relnamespace
                     WHERE n.nspname = 'public'
                     ORDER BY c.relname
                     """)) {
            while (rows.next()) {
                sql.append("CREATE SEQUENCE public.").append(q(rows.getString(1)))
                        .append(" AS ").append(rows.getString(2))
                        .append(" START WITH ").append(rows.getLong(3))
                        .append(" INCREMENT BY ").append(rows.getLong(4))
                        .append(" MINVALUE ").append(rows.getLong(5))
                        .append(" MAXVALUE ").append(rows.getLong(6))
                        .append(" CACHE ").append(rows.getLong(7));
                if (rows.getBoolean(8)) sql.append(" CYCLE");
                sql.append(";\n");
            }
            sql.append('\n');
        }
    }

    private static List<String> listTables(Connection connection) throws Exception {
        List<String> tables = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     SELECT tablename FROM pg_tables
                     WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
                     ORDER BY tablename
                     """)) {
            while (rows.next()) tables.add(rows.getString(1));
        }
        return tables;
    }

    private static void appendTable(Connection connection, StringBuilder sql, String table) throws Exception {
        sql.append("CREATE TABLE public.").append(q(table)).append(" (\n");
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT a.attname, format_type(a.atttypid, a.atttypmod), a.attnotnull,
                       pg_get_expr(d.adbin, d.adrelid), a.attidentity, a.attgenerated
                FROM pg_attribute a
                JOIN pg_class c ON c.oid = a.attrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                LEFT JOIN pg_attrdef d ON d.adrelid = a.attrelid AND d.adnum = a.attnum
                WHERE n.nspname = 'public' AND c.relname = ?
                  AND a.attnum > 0 AND NOT a.attisdropped
                ORDER BY a.attnum
                """)) {
            statement.setString(1, table);
            try (ResultSet rows = statement.executeQuery()) {
                boolean first = true;
                while (rows.next()) {
                    if (!first) sql.append(",\n");
                    first = false;
                    sql.append("    ").append(q(rows.getString(1))).append(' ').append(rows.getString(2));
                    String identity = rows.getString(5);
                    String generated = rows.getString(6);
                    String defaultExpression = rows.getString(4);
                    if (identity != null && !identity.isEmpty()) {
                        sql.append(" GENERATED ")
                                .append("a".equals(identity) ? "ALWAYS" : "BY DEFAULT")
                                .append(" AS IDENTITY");
                    } else if (generated != null && !generated.isEmpty()) {
                        sql.append(" GENERATED ALWAYS AS (").append(defaultExpression).append(") STORED");
                    } else if (defaultExpression != null) {
                        sql.append(" DEFAULT ").append(defaultExpression);
                    }
                    if (rows.getBoolean(3)) sql.append(" NOT NULL");
                }
            }
        }
        sql.append("\n);\n\n");
    }

    private static void appendConstraints(Connection connection, StringBuilder sql, boolean foreignKeys)
            throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT c.relname, con.conname, pg_get_constraintdef(con.oid, true)
                FROM pg_constraint con
                JOIN pg_class c ON c.oid = con.conrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public'
                  AND ((con.contype = 'f') = ?)
                ORDER BY c.relname, con.conname
                """)) {
            statement.setBoolean(1, foreignKeys);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    sql.append("ALTER TABLE ONLY public.").append(q(rows.getString(1)))
                            .append(" ADD CONSTRAINT ").append(q(rows.getString(2)))
                            .append(' ').append(rows.getString(3)).append(";\n");
                }
            }
        }
        sql.append('\n');
    }

    private static void appendSequenceOwnership(Connection connection, StringBuilder sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     SELECT seq.relname, tbl.relname, att.attname
                     FROM pg_class seq
                     JOIN pg_namespace ns ON ns.oid = seq.relnamespace
                     JOIN pg_depend dep ON dep.objid = seq.oid AND dep.deptype IN ('a', 'i')
                     JOIN pg_class tbl ON tbl.oid = dep.refobjid
                     JOIN pg_attribute att ON att.attrelid = tbl.oid AND att.attnum = dep.refobjsubid
                     WHERE ns.nspname = 'public' AND seq.relkind = 'S'
                     ORDER BY seq.relname
                     """)) {
            while (rows.next()) {
                sql.append("ALTER SEQUENCE public.").append(q(rows.getString(1)))
                        .append(" OWNED BY public.").append(q(rows.getString(2)))
                        .append('.').append(q(rows.getString(3))).append(";\n");
            }
        }
        sql.append('\n');
    }

    private static void appendIndexes(Connection connection, StringBuilder sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     SELECT pg_get_indexdef(i.indexrelid)
                     FROM pg_index i
                     JOIN pg_class idx ON idx.oid = i.indexrelid
                     JOIN pg_class tbl ON tbl.oid = i.indrelid
                     JOIN pg_namespace n ON n.oid = tbl.relnamespace
                     LEFT JOIN pg_constraint con ON con.conindid = i.indexrelid
                     WHERE n.nspname = 'public' AND con.oid IS NULL
                     ORDER BY idx.relname
                     """)) {
            while (rows.next()) sql.append(rows.getString(1)).append(";\n");
        }
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
}
