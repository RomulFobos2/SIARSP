package com.mai.siarsp.enumeration;

import lombok.Getter;

@Getter
public enum AttributeType {
    TEXT("Текст"),       // Обычный текст
    NUMBER("Число"),     // Число (целое или дробное)
    DATE("Дата");       // Дата

    private final String displayName;

    AttributeType(String displayName) {
        this.displayName = displayName;
    }

}
