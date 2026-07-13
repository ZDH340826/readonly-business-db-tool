package com.local.monitor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class PointDataQueryRepositoryTest {
    public static void main(String[] args) throws Exception {
        localStructuredQueryUsesFixedReadOnlySourceAndFilters();
        paginatesWithCountAndOffset();
        likeWildcardsAreTreatedAsLiteralText();
        rejectsFreeSqlAndInvalidIdentifiers();
        System.out.println("PointDataQueryRepositoryTest PASS");
    }

    private static void localStructuredQueryUsesFixedReadOnlySourceAndFilters() throws Exception {
        Path dbPath = Files.createTempDirectory("point-query-test").resolve("local-test-db");
        DbConfig config = DbConfig.localTest(dbPath.toString(), 10);
        LocalTestDatabase.reset(config);
        PointDataQueryRepository repository = new PointDataQueryRepository();
        PointDataQuery query = new PointDataQuery(
                "USE_POINT",
                "",
                "AREA_USE",
                "",
                "",
                "",
                20,
                0);

        PointDataQueryResult result = repository.query(config, new char[0], query);

        TestSupport.assertEquals(1, result.records().size(), "area and point keyword filters should narrow rows");
        TestSupport.assertEquals("USE_POINT_001", result.records().get(0).mapDataCode(), "query should return point");
        TestSupport.assertEquals("select", result.sqlKind(), "query should expose SELECT-only kind");
        TestSupport.assertFalse(result.sqlTemplate().contains("USE_POINT"),
                "SQL template must not contain filter values");
        TestSupport.assertFalse(result.sqlTemplate().contains("AREA_USE"),
                "SQL template must not contain area values");
        TestSupport.assertTrue(result.sqlTemplate().contains(" from public.tcs_map_data "),
                "query should use the fixed point status source table");
        TestSupport.assertTrue(result.sqlTemplate().contains(" limit ? offset ?"),
                "query should use parameterized pagination");
        TestSupport.assertTrue(result.countSqlTemplate().contains("count(*)"),
                "query should expose parameterized count SQL");
    }

    private static void paginatesWithCountAndOffset() throws Exception {
        Path dbPath = Files.createTempDirectory("point-query-page-test").resolve("local-test-db");
        DbConfig config = DbConfig.localTest(dbPath.toString(), 10);
        LocalTestDatabase.reset(config);
        PointDataQueryRepository repository = new PointDataQueryRepository();

        PointDataQuery firstPage = new PointDataQuery("", "", "", "", "", "", 2, 0);
        PointDataQueryResult first = repository.query(config, new char[0], firstPage);
        TestSupport.assertEquals(5, first.totalCount(), "count query should return all matching rows");
        TestSupport.assertEquals(2, first.records().size(), "first page should use page size");
        TestSupport.assertEquals(0, first.offset(), "first page offset should be zero");

        PointDataQuery secondPage = new PointDataQuery("", "", "", "", "", "", 2, 2);
        PointDataQueryResult second = repository.query(config, new char[0], secondPage);
        TestSupport.assertEquals(5, second.totalCount(), "second page should keep the same total");
        TestSupport.assertEquals(2, second.records().size(), "second page should use offset");
        TestSupport.assertEquals(2, second.offset(), "second page offset should be two");

        PointDataQuery emptyPage = new PointDataQuery("NO_SUCH_POINT", "", "", "", "", "", 2, 0);
        PointDataQueryResult empty = repository.query(config, new char[0], emptyPage);
        TestSupport.assertEquals(0, empty.totalCount(), "empty result should have zero total count");
        TestSupport.assertEquals(0, empty.records().size(), "empty result should return no rows");
    }

    private static void likeWildcardsAreTreatedAsLiteralText() throws Exception {
        Path dbPath = Files.createTempDirectory("point-query-like-test").resolve("local-test-db");
        DbConfig config = DbConfig.localTest(dbPath.toString(), 10);
        LocalTestDatabase.reset(config);
        PointDataQueryRepository repository = new PointDataQueryRepository();

        PointDataQuery percent = new PointDataQuery("USE%", "", "", "", "", "", 20, 0);
        PointDataQuery underscore = new PointDataQuery("USE_POINT_01", "", "", "", "", "", 20, 0);
        PointDataQuery slash = new PointDataQuery("USE\\", "", "", "", "", "", 20, 0);

        TestSupport.assertEquals(0, repository.query(config, new char[0], percent).records().size(),
                "percent should be literal, not a wildcard");
        TestSupport.assertEquals(0, repository.query(config, new char[0], underscore).records().size(),
                "underscore should be literal, not a wildcard");
        TestSupport.assertEquals(0, repository.query(config, new char[0], slash).records().size(),
                "backslash should be literal, not an escape leak");
    }

    private static void rejectsFreeSqlAndInvalidIdentifiers() {
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> PointDataQuery.fixedSelectSql("public;drop"),
                "invalid schema should be rejected");
        String sql = PointDataQuery.fixedSelectSql("public");
        TestSupport.assertFalse(containsSqlWord(sql, "insert"), "query SQL must not contain insert");
        TestSupport.assertFalse(containsSqlWord(sql, "update"), "query SQL must not contain update");
        TestSupport.assertFalse(containsSqlWord(sql, "delete"), "query SQL must not contain delete");
        TestSupport.assertFalse(sql.toLowerCase().contains(" drop "), "query SQL must not contain ddl");
        String countSql = PointDataQuery.fixedCountSql("public");
        TestSupport.assertTrue(countSql.toLowerCase().contains("select count(*)"),
                "count SQL should be SELECT COUNT");
        TestSupport.assertFalse(containsSqlWord(countSql, "insert"), "count SQL must not contain insert");
        TestSupport.assertFalse(containsSqlWord(countSql, "update"), "count SQL must not contain update");
        TestSupport.assertFalse(containsSqlWord(countSql, "delete"), "count SQL must not contain delete");
    }

    private static boolean containsSqlWord(String sql, String word) {
        return sql.toLowerCase().matches(".*\\b" + word + "\\b.*");
    }

    private static final class TestSupport {
        static void assertTrue(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }

        static void assertFalse(boolean condition, String message) {
            if (condition) {
                throw new AssertionError(message);
            }
        }

        static void assertEquals(Object expected, Object actual, String message) {
            if (!java.util.Objects.equals(expected, actual)) {
                throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
            }
        }

        static void assertThrows(Class<? extends Throwable> type, ThrowingRunnable action, String message) {
            try {
                action.run();
            } catch (Throwable t) {
                if (type.isInstance(t)) {
                    return;
                }
                throw new AssertionError(message + " wrong exception=" + t);
            }
            throw new AssertionError(message + " no exception thrown");
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
