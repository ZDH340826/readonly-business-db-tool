package com.local.monitor;

public final class LocalTestDbTool {
    private LocalTestDbTool() {
    }

    public static void main(String[] args) throws Exception {
        String scenario = args.length > 0 ? args[0] : "missing-use";
        String path = args.length > 1 ? args[1] : "data/local-test-db";
        DbConfig config = DbConfig.localTest(path, 30);
        if ("reset".equalsIgnoreCase(scenario)) {
            LocalTestDatabase.reset(config);
            System.out.println("local test database reset: " + path);
        } else {
            LocalTestDatabase.setScenario(config, scenario);
            System.out.println("local test database scenario=" + scenario + ": " + path);
        }
    }
}


