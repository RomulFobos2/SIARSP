package com.mai.siarsp.enumeration;

public enum VehicleType {
    STANDARD("Обычный"),
    REFRIGERATED("Рефрижератор");

    private final String displayName;

    VehicleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
