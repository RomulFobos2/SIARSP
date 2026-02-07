package com.mai.siarsp.enumeration;

import lombok.Getter;

@Getter
public enum VehicleType {
    STANDARD("Обычный"),
    REFRIGERATED("Рефрижератор");

    private final String displayName;

    VehicleType(String displayName) {
        this.displayName = displayName;
    }

}
