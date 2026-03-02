package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.VehicleStatus;
import com.mai.siarsp.enumeration.VehicleType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Автомобиль для доставки товаров
 *
 * Транспортное средство, используемое для перевозки товаров от склада ИП "Левчук"
 * до конечных потребителей (детские сады, школы, больницы).
 *
 * Типы автомобилей в системе:
 * 1. STANDARD - обычный грузовой автомобиль (для товаров стандартного хранения)
 * 2. REFRIGERATED - рефрижератор (для скоропортящихся продуктов)
 *
 * Статусы автомобиля:
 * - AVAILABLE - доступен для новых доставок
 * - IN_USE - используется в текущей доставке
 * - MAINTENANCE - на техническом обслуживании
 * - BROKEN - неисправен, требует ремонта
 * - DECOMMISSIONED - списан, не используется
 *
 * Использование в бизнес-процессе:
 * 1. Заведующий складом создает задачу на доставку (DeliveryTask)
 * 2. Выбирает подходящий автомобиль (доступный, нужного типа, достаточной вместимости)
 * 3. Назначает водителя
 * 4. Водитель выполняет доставку, фиксирует пробег
 * 5. После завершения доставки автомобиль снова становится доступным
 * 6. Периодически проводится техническое обслуживание
 *
 * Связи:
 * - Используется во множестве задач на доставку (DeliveryTask)
 * - Указывается в товарно-транспортных накладных (TTN)
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_vehicle")
@EqualsAndHashCode(of = "id")
public class Vehicle {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Регистрационный номер автомобиля (государственный номер)
     * Уникальный номер транспортного средства, выданный ГИБДД
     * Уникален в системе - один номер для одного автомобиля
     */
    @Column(nullable = false, unique = true, length = 20)
    private String registrationNumber;

    /**
     * Марка автомобиля (производитель)
     *
     */
    @Column(nullable = false, length = 100)
    private String brand;

    /**
     * Модель автомобиля
     *
     */
    @Column(nullable = false, length = 100)
    private String model;

    /**
     * Год выпуска автомобиля
     */
    @Column
    private Integer year;

    /**
     * VIN-код автомобиля (Vehicle Identification Number)
     */
    @Column(length = 50)
    private String vin;

    /**
     * Грузоподъемность автомобиля в килограммах
     * Максимальный вес груза, который может перевозить автомобиль
     * Важно:
     * Перед назначением автомобиля на доставку система должна проверить:
     * totalWeight (из TTN) ≤ loadCapacity (Vehicle)
     */
    @Column(nullable = false)
    private Double loadCapacity;

    /**
     * Объемная вместимость кузова в кубических метрах
     * Максимальный объем груза, который может поместиться в кузове
     * Важно:
     * Система должна проверять ОБА параметра (вес И объем):
     * - totalVolume (TTN) ≤ volumeCapacity (Vehicle)
     * - totalWeight (TTN) ≤ loadCapacity (Vehicle)
     *
     * Пример:
     * Груз весит 500 кг (в пределах грузоподъемности 1500 кг),
     * но объем 12 м³ не помещается в кузов объемом 9 м³
     * → нужен автомобиль большей вместимости
     */
    @Column(nullable = false)
    private Double volumeCapacity;

    /**
     * Тип автомобиля
     *
     * Возможные типы (см. VehicleType enum):
     * - STANDARD - обычный грузовой автомобиль
     * - REFRIGERATED - рефрижератор (с холодильной установкой)
     *
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType type;

    /**
     * Статус автомобиля
     *
     * Переходы статусов:
     * AVAILABLE → IN_USE (при назначении на доставку)
     * IN_USE → AVAILABLE (после завершения доставки)
     * AVAILABLE → MAINTENANCE (при плановом ТО)
     * MAINTENANCE → AVAILABLE (после завершения ТО)
     * AVAILABLE → BROKEN (при поломке)
     * BROKEN → MAINTENANCE (при начале ремонта)
     * MAINTENANCE → AVAILABLE (после ремонта)
     * любой → DECOMMISSIONED (при списании)
     *
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    /**
     * Текущий пробег автомобиля в километрах
     * Обновляется при завершении каждой доставки (endMileage → currentMileage)
     * При начале доставки startMileage должен быть >= currentMileage
     */
    @Column(nullable = false)
    private Integer currentMileage = 0;

    /**
     * История задач на доставку, выполненных на этом автомобиле
     * Список всех доставок, в которых использовался данный автомобиль
     *
     * Каждая задача (DeliveryTask) содержит:
     * - Заказ клиента
     * - Водителя
     * - Маршрут с контрольными точками
     * - Пробег (начальный и конечный)
     * - Дату и время доставки
     *
     * Используется для:
     * - Анализа интенсивности использования автомобиля
     * - Расчета общего пробега
     * - Планирования ТО (пробег влияет на частоту ТО)
     * - Статистики по доставкам
     * - Оценки загруженности автомобиля
     * - Определения необходимости покупки нового автомобиля
     *
     * Пример анализа:
     * Автомобиль "ГАЗель Next А123БВ777":
     * - 45 доставок за месяц
     * - Средний пробег за доставку: 15 км
     * - Общий пробег за месяц: 675 км
     * - Загруженность: высокая (используется почти каждый день)
     * → Нужно запланировать ТО в ближайшее время
     */
    @ToString.Exclude
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryTask> deliveryTasks = new ArrayList<>();

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создает новый автомобиль с основными характеристиками
     * Автоматически устанавливает начальный статус AVAILABLE (доступен)
     *
     * @param registrationNumber государственный регистрационный номер (например, "А123БВ777")
     * @param brand марка автомобиля (например, "ГАЗ")
     * @param model модель автомобиля (например, "ГАЗель Next")
     * @param loadCapacity грузоподъемность в килограммах (например, 1500.0)
     * @param volumeCapacity объемная вместимость кузова в м³ (например, 9.0)
     * @param type тип автомобиля (STANDARD или REFRIGERATED)
     */
    public Vehicle(String registrationNumber, String brand, String model,
                   Double loadCapacity, Double volumeCapacity, VehicleType type) {
        this.registrationNumber = registrationNumber;
        this.brand = brand;
        this.model = model;
        this.loadCapacity = loadCapacity;
        this.volumeCapacity = volumeCapacity;
        this.type = type;
        this.status = VehicleStatus.AVAILABLE;
    }

    // ========== МЕТОДЫ ==========

    /**
     * Возвращает полное название автомобиля с номером
     * Комбинирует марку, модель и регистрационный номер
     *
     * Примеры:
     * - "ГАЗ ГАЗель Next (А123БВ777)"
     * - "Hyundai Porter II (В456ГД199)"
     * - "Mercedes-Benz Sprinter (Т789ЕЖ750)"
     *
     */
    @Transient
    public String getFullName() {
        return brand + " " + model + " (" + registrationNumber + ")";
    }

    /**
     * Проверяет доступность автомобиля для новой доставки
     */
    @Transient
    public boolean isAvailable() {
        return status == VehicleStatus.AVAILABLE;
    }

}