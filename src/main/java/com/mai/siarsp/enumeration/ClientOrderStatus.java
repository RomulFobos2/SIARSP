package com.mai.siarsp.enumeration;

import lombok.Getter;

@Getter
public enum ClientOrderStatus {
    NEW("Новый"),
    CONFIRMED("Подтвержден"),
    RESERVED("Товар зарезервирован"),
    IN_PROGRESS("В работе"),
    READY("Готов к отгрузке"),
    SHIPPED("Отгружен"),
    DELIVERED("Доставлен"),
    CANCELLED("Отменен");

    private final String displayName;

    ClientOrderStatus(String displayName) {
        this.displayName = displayName;
    }

}
