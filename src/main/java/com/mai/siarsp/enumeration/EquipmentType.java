package com.mai.siarsp.enumeration;

import lombok.Getter;

@Getter
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

}
