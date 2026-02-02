package com.mai.siarsp.enumeration;

public enum VehicleStatus {
    AVAILABLE("Готов к работе"),
    IN_USE("В работе"),
    MAINTENANCE("На обслуживании"),
    BROKEN("Неисправен"),
    DECOMMISSIONED("Списан");

    private final String displayName;

    VehicleStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
