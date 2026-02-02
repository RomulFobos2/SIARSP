package com.mai.siarsp.enumeration;

public enum RequestStatus {
    DRAFT("Черновик"),
    PENDING_DIRECTOR("На согласовании у директора"),
    REJECTED_BY_DIRECTOR("Отклонено директором"),
    PENDING_ACCOUNTANT("На согласовании у бухгалтера"),
    REJECTED_BY_ACCOUNTANT("Отклонено бухгалтером"),
    APPROVED("Согласовано"),
    PARTIALLY_RECEIVED("Частично получено"),
    RECEIVED("Получено"),
    CANCELLED("Отменено");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}








