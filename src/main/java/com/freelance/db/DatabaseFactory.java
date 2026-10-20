package com.freelance.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseFactory {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseFactory.class);
    private static Properties props = new Properties();
    private static String dbType = "h2";
    private static String url;
    private static String user;
    private static String password;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        File propFile = new File("db.properties");
        try {
            if (propFile.exists()) {
                try (InputStream is = new FileInputStream(propFile)) {
                    props.load(is);
                }
            } else {
                try (InputStream is = DatabaseFactory.class.getClassLoader().getResourceAsStream("db.properties")) {
                    if (is != null) {
                        props.load(is);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not load db.properties, falling back to embedded H2: {}", e.getMessage());
        }

        dbType = System.getProperty("db.type", props.getProperty("db.type", "h2")).toLowerCase();

        if ("postgres".equals(dbType) || "postgresql".equals(dbType)) {
            url = System.getProperty("db.postgres.url", props.getProperty("db.postgres.url", "jdbc:postgresql://localhost:5432/freelance_db"));
            user = System.getProperty("db.postgres.user", props.getProperty("db.postgres.user", "postgres"));
            password = System.getProperty("db.postgres.password", props.getProperty("db.postgres.password", "postgres"));
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                logger.error("PostgreSQL Driver not found: {}", e.getMessage());
            }
        } else {
            dbType = "h2";
            url = System.getProperty("db.h2.url", props.getProperty("db.h2.url", "jdbc:h2:./freelance_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE"));
            user = System.getProperty("db.h2.user", props.getProperty("db.h2.user", "sa"));
            password = System.getProperty("db.h2.password", props.getProperty("db.h2.password", ""));
            try {
                Class.forName("org.h2.Driver");
            } catch (ClassNotFoundException e) {
                logger.error("H2 Driver not found: {}", e.getMessage());
            }
        }
        logger.info("Configured Database Mode: {} -> {}", dbType, url);
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public static String getDbType() {
        return dbType;
    }
}
