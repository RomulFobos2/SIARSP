package com.mai.siarsp.enumeration;

import lombok.Getter;

@Getter
public enum WarehouseType {
    REGULAR("Обычный склад"),
    REFRIGERATOR("Холодильная камера");

    private final String displayName;

    WarehouseType(String displayName) {
        this.displayName = displayName;
    }

}
