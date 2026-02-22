package com.mai.siarsp.enumeration;

import lombok.Getter;

/**
 * Статус акта списания товара
 *
 * Жизненный цикл акта:
 * 1. Заведующий складом создаёт акт → PENDING_DIRECTOR
 * 2. Директор утверждает → APPROVED (товар списывается)
 *    или отклоняет → REJECTED
 */
@Getter
public enum WriteOffActStatus {
    PENDING_DIRECTOR("На подписи у директора"),
    APPROVED("Утверждён"),
    REJECTED("Отклонён");

    private final String displayName;

    WriteOffActStatus(String displayName) {
        this.displayName = displayName;
    }
}
