package com.mai.siarsp.enumeration;

public enum RoutePointType {
    WAREHOUSE("Склад"),
    DELIVERY_ADDRESS("Адрес доставки"),
    CHECKPOINT("Контрольная точка");

    private final String displayName;

    RoutePointType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
