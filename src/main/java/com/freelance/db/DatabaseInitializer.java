package com.freelance.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    public static void initialize() throws SQLException {
        try (Connection conn = DatabaseFactory.getConnection();
             InputStream is = DatabaseInitializer.class.getClassLoader().getResourceAsStream("sql/schema.sql")) {
            if (is == null) {
                throw new SQLException("Database schema resource sql/schema.sql was not found");
            }

            String sql;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                sql = reader.lines().collect(Collectors.joining("\n"));
            }

            // Remove line comments before splitting.  Otherwise a statement following a
            // '-- comment' can be submitted as a comment-only command and silently skipped.
            String executableSql = sql.lines()
                    .filter(line -> !line.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"));
            String[] statements = executableSql.split(";");
            try (Statement stmt = conn.createStatement()) {
                for (String s : statements) {
                    String trimmed = s.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }
            logger.info("Database schema initialized successfully.");
        } catch (IOException e) {
            throw new SQLException("Failed to read database schema resource", e);
        }
    }
}
