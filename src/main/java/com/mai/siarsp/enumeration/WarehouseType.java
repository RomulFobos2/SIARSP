package com.mai.siarsp.enumeration;

public enum WarehouseType {
    REGULAR("Обычный склад"),
    REFRIGERATOR("Холодильная камера");

    private final String displayName;

    WarehouseType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
