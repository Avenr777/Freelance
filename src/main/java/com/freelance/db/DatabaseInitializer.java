package com.freelance.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    public static void initialize() {
        try (Connection conn = DatabaseFactory.getConnection()) {
            InputStream is = DatabaseInitializer.class.getClassLoader().getResourceAsStream("sql/schema.sql");
            if (is == null) {
                logger.error("schema.sql resource file not found!");
                return;
            }

            String sql;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                sql = reader.lines().collect(Collectors.joining("\n"));
            }

            // Split by semicolon for multiple statements
            String[] statements = sql.split(";");
            try (Statement stmt = conn.createStatement()) {
                for (String s : statements) {
                    String trimmed = s.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }
            logger.info("Database schema initialized successfully.");
        } catch (Exception e) {
            logger.error("Failed to initialize database schema: {}", e.getMessage(), e);
        }
    }
}
