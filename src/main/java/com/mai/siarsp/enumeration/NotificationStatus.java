package com.mai.siarsp.enumeration;

import lombok.Getter;

@Getter
public enum NotificationStatus {
    NEW("Новое"),
    READ("Прочитано");

    private final String displayName;

    NotificationStatus(String displayName) {
        this.displayName = displayName;
    }
}
