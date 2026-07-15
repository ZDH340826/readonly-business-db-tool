package com.local.monitor;

final class DatabaseFieldLabel {
    private DatabaseFieldLabel() {
    }

    static String display(String identifier) {
        if ("map_data_code".equalsIgnoreCase(identifier)) {
            return "<html><b>地码</b><br><small>map_data_code</small></html>";
        }
        if ("pod_code".equalsIgnoreCase(identifier)) {
            return "<html><b>货码</b><br><small>pod_code</small></html>";
        }
        return identifier == null ? "" : identifier;
    }
}
