package com.mai.siarsp.enumeration;

public enum WriteOffReason {
    DEFECT("Брак"),
    EXPIRED("Истек срок годности"),
    DAMAGE("Повреждение при хранении"),
    LOSS("Недостача"),
    OTHER("Прочее");

    private final String displayName;

    WriteOffReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
