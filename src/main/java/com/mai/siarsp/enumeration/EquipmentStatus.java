package com.mai.siarsp.enumeration;

import lombok.Getter;

/**
 * Статус оборудования склада.
 *
 * Определяет текущее состояние единицы оборудования:
 * - IN_USE: оборудование находится в активной эксплуатации
 * - UNDER_REPAIR: оборудование отправлено на техническое обслуживание/ремонт
 * - WRITTEN_OFF: оборудование списано и выведено из эксплуатации
 */
public enum EquipmentStatus {

    IN_USE("В эксплуатации"),
    UNDER_REPAIR("На ремонте"),
    WRITTEN_OFF("Списан");

    @Getter
    private final String displayName;

    EquipmentStatus(String displayName) {
        this.displayName = displayName;
    }
}
