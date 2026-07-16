package com.local.monitor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

public final class ReadOnlyConnectionTest {
    private static final List<Path> PRODUCTION_DATABASE_SOURCES = List.of(
            Path.of("src", "com", "local", "monitor", "ReadOnlyConnectionFactory.java"),
            Path.of("src", "com", "local", "monitor", "PointRepository.java"),
            Path.of("src", "com", "local", "monitor", "PointDataQueryRepository.java"),
            Path.of("src", "com", "local", "monitor", "PointQuery.java"),
            Path.of("src", "com", "local", "monitor", "PointDataQuery.java"),
            Path.of("src", "com", "local", "monitor", "DbMetadataRepository.java"));
    private static final Pattern WRITE_SQL_LITERAL = Pattern.compile(
            "(?i)\\\"\\s*(?:insert|update|delete|merge|truncate|alter|drop|create|grant|revoke)\\b");

    private ReadOnlyConnectionTest() {
    }

    public static void main(String[] args) throws Exception {
        factoryMarksLocalConnectionReadOnly();
        productionDatabaseSourcesContainNoWriteSqlLiteral();
        factoryEnforcesPostgresReadOnlyTransaction();
        System.out.println("ReadOnlyConnectionTest PASS");
    }

    private static void factoryMarksLocalConnectionReadOnly() throws Exception {
        Path database = Files.createTempDirectory("read-only-connection").resolve("monitor");
        DbConfig config = DbConfig.localTest(database.toString(), 30);

        try (Connection connection = ReadOnlyConnectionFactory.open(config, new char[0])) {
            assertTrue(connection.isReadOnly(), "factory connection must be marked read-only");
            assertWriteRejected(connection);
        }
    }

    private static void assertWriteRejected(Connection connection) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute("create table forbidden_field_write(id integer)");
            throw new AssertionError("factory connection must reject database writes");
        } catch (SQLException expected) {
            assertContains(expected.getMessage(), "read only", "H2 must reject writes at the database boundary");
        }
    }

    private static void productionDatabaseSourcesContainNoWriteSqlLiteral() throws Exception {
        for (Path source : PRODUCTION_DATABASE_SOURCES) {
            String text = Files.readString(source, StandardCharsets.UTF_8);
            if (WRITE_SQL_LITERAL.matcher(text).find()) {
                throw new AssertionError("production database source contains a write SQL literal: " + source);
            }
        }
    }

    private static void factoryEnforcesPostgresReadOnlyTransaction() throws Exception {
        String source = Files.readString(PRODUCTION_DATABASE_SOURCES.get(0), StandardCharsets.UTF_8);
        assertContains(source, "conn.setReadOnly(true)", "JDBC connection must be marked read-only");
        assertContains(source, "SET TRANSACTION READ ONLY", "PostgreSQL transaction must be explicitly read-only");
        assertContains(source, "SET statement_timeout='8s'", "PostgreSQL query timeout must remain bounded");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertContains(String text, String expected, String message) {
        if (!text.contains(expected)) {
            throw new AssertionError(message + " expected=" + expected);
        }
    }
}
