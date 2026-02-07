package com.mai.siarsp.enumeration;

import lombok.Getter;

@Getter
public enum RoutePointType {
    WAREHOUSE("Склад"),
    DELIVERY_ADDRESS("Адрес доставки"),
    CHECKPOINT("Контрольная точка");

    private final String displayName;

    RoutePointType(String displayName) {
        this.displayName = displayName;
    }

}
