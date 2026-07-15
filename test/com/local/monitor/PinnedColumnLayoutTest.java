package com.local.monitor;

import java.util.List;

public final class PinnedColumnLayoutTest {
    private PinnedColumnLayoutTest() {
    }

    public static void main(String[] args) {
        presetPinsLandCodeBeforePodCode();
        missingPresetColumnDisablesThePreset();
        normalizeIgnoresMissingAndDuplicateColumns();
        normalizeRejectsMoreThanFourColumnsInChinese();
        movesSelectedColumnsWithoutMutatingInput();
        System.out.println("PinnedColumnLayoutTest PASS");
    }

    private static void presetPinsLandCodeBeforePodCode() {
        List<String> available = List.of("status", "map_data_code", "date_chg", "pod_code");
        TestSupport.assertEquals(
                List.of("map_data_code", "pod_code"),
                PinnedColumnLayout.presetFor(available),
                "preset must pin land code immediately before pod code");
    }

    private static void missingPresetColumnDisablesThePreset() {
        TestSupport.assertEquals(
                List.of(),
                PinnedColumnLayout.presetFor(List.of("map_data_code", "status")),
                "preset requires both land code and pod code");
    }

    private static void normalizeIgnoresMissingAndDuplicateColumns() {
        TestSupport.assertEquals(
                List.of("POD_CODE", "status"),
                PinnedColumnLayout.normalize(
                        List.of("MAP_DATA_CODE", "POD_CODE", "status"),
                        List.of("missing", "pod_code", "POD_CODE", "status")),
                "saved layouts must tolerate schema changes and preserve actual identifiers");
    }

    private static void normalizeRejectsMoreThanFourColumnsInChinese() {
        IllegalArgumentException error = TestSupport.assertThrowsReturning(
                IllegalArgumentException.class,
                () -> PinnedColumnLayout.normalize(
                        List.of("a", "b", "c", "d", "e"),
                        List.of("a", "b", "c", "d", "e")),
                "more than four fixed columns must be rejected");
        TestSupport.assertEquals(
                "固定列太多，请先取消一列",
                error.getMessage(),
                "operator must receive a direct Chinese recovery message");
    }

    private static void movesSelectedColumnsWithoutMutatingInput() {
        List<String> original = List.of("map_data_code", "status", "pod_code");
        TestSupport.assertEquals(
                List.of("map_data_code", "pod_code", "status"),
                PinnedColumnLayout.moveUp(original, 2),
                "move up must change only the returned order");
        TestSupport.assertEquals(
                List.of("status", "map_data_code", "pod_code"),
                PinnedColumnLayout.moveDown(original, 0),
                "move down must change only the returned order");
        TestSupport.assertEquals(
                List.of("map_data_code", "status", "pod_code"),
                original,
                "move operations must not mutate caller state");
    }

    private static final class TestSupport {
        private static void assertEquals(Object expected, Object actual, String message) {
            if (!java.util.Objects.equals(expected, actual)) {
                throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
            }
        }

        private static <T extends Throwable> T assertThrowsReturning(
                Class<T> type,
                ThrowingRunnable action,
                String message) {
            try {
                action.run();
            } catch (Throwable error) {
                if (type.isInstance(error)) {
                    return type.cast(error);
                }
                throw new AssertionError(message + " wrong exception=" + error, error);
            }
            throw new AssertionError(message + " no exception thrown");
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
