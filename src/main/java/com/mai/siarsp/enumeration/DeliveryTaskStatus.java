package com.mai.siarsp.enumeration;

import lombok.Getter;

@Getter
public enum DeliveryTaskStatus {
    PENDING("Ожидает выполнения"),
    LOADING("Идет погрузка"),
    LOADED("Ожидает отправки"),
    IN_TRANSIT("В пути"),
    DELIVERED("Доставлено"),
    CANCELLED("Отменено");

    private final String displayName;

    DeliveryTaskStatus(String displayName) {
        this.displayName = displayName;
    }

}
