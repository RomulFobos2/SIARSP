package com.mai.siarsp.enumeration;

public enum OrderStatus {
    NEW("Новый заказ"),                          // Пользователь оформил заказ
    WAITING_FOR_DELIVERY("Ожидается поставка"),  // Менеджер ждёт поступления товаров
    IN_EXECUTION("На исполнении"),               // Заказ в сборке, хватает товара
    READY_FOR_SHIPMENT("Готово для доставки"),   // Склад собрал, ждёт курьера
    READY_FOR_CLIENT("Ожидает клиента"),   // Склад собрал, ждёт клиента когда Самовывоз
    CANCELED("Отменено"),                        // Отмена по любой причине
    DELIVERED("Доставлено");                     // Заказ доставлен клиенту

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

