package com.mai.siarsp.enumeration;

public enum RequestStatus {
    PENDING ("Не отправлено"),
    SENT("Отправлено"),      // отправлено поставщику
    RECEIVED("Выполнено"),  // получен ответ/товар
    CANCELLED ("Отменено"); // отменён

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}








