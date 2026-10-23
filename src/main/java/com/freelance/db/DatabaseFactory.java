package com.freelance.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseFactory {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseFactory.class);
    private static final Properties props = new Properties();
    private static String dbType = "h2";
    private static String url;
    private static String user;
    private static String password;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream is = DatabaseFactory.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is != null) {
                props.load(is);
            }
            java.io.File propFile = new java.io.File("db.properties");
            if (propFile.isFile()) {
                try (InputStream fileInput = new java.io.FileInputStream(propFile)) {
                    props.load(fileInput);
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not load database configuration", e);
        }

        dbType = System.getProperty("db.type", props.getProperty("db.type", "h2")).toLowerCase();

        if ("postgres".equals(dbType) || "postgresql".equals(dbType)) {
            url = System.getProperty("db.postgres.url", props.getProperty("db.postgres.url", "jdbc:postgresql://localhost:5432/freelance_db"));
            user = System.getProperty("db.postgres.user", props.getProperty("db.postgres.user", "postgres"));
            password = System.getProperty("db.postgres.password", props.getProperty("db.postgres.password", "postgres"));
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("PostgreSQL JDBC driver is not available", e);
            }
            dbType = "postgres";
        } else if ("h2".equals(dbType)) {
            url = System.getProperty("db.h2.url", props.getProperty("db.h2.url", "jdbc:h2:./freelance_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE"));
            user = System.getProperty("db.h2.user", props.getProperty("db.h2.user", "sa"));
            password = System.getProperty("db.h2.password", props.getProperty("db.h2.password", ""));
            try {
                Class.forName("org.h2.Driver");
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("H2 JDBC driver is not available", e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
        logger.info("Configured JDBC database mode: {}", dbType);
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public static String getDbType() {
        return dbType;
    }
}
