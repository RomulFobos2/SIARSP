package com.mai.siarsp.enumeration;

public enum DeliveryTaskStatus {
    PENDING("Ожидает выполнения"),
    LOADING("Идет погрузка"),
    IN_TRANSIT("В пути"),
    DELIVERED("Доставлено"),
    CANCELLED("Отменено");

    private final String displayName;

    DeliveryTaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
