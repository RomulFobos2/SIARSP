package com.mai.siarsp.enumeration;

public enum EquipmentType {
    RACK("Стеллаж"),
    FRIDGE("Холодильная камера"),
    PALLET("Поддон"),
    SCALE("Весы"),
    FORKLIFT("Погрузчик"),
    OTHER("Прочее");

    private final String displayName;

    EquipmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
