package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.DeliveryTaskStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Задача на доставку товара клиенту
 *
 * Центральная сущность бизнес-процесса доставки товара конечному потребителю.
 * Создается заведующим складом после комплектации заказа. Содержит информацию о маршруте,
 * транспортном средстве, водителе, контрольных точках и статусе выполнения.
 * Используется водителем-экспедитором через мобильное приложение для выполнения доставки.
 *
 * Бизнес-процесс:
 * 1. Заведующий складом формирует комплектацию заказа
 * 2. Создает задачу на доставку (DeliveryTask) с маршрутом и контрольными точками
 * 3. Назначает водителя и автомобиль
 * 4. Водитель видит задачу в мобильном приложении
 * 5. Начинает выполнение (вводит начальный пробег, статус → IN_TRANSIT)
 * 6. Приложение отслеживает геолокацию, отмечает прохождение контрольных точек
 * 7. По завершении доставки (вводит конечный пробег, статус → DELIVERED)
 *
 * Связи:
 * - Одна задача для одного заказа клиента (ClientOrder)
 * - Выполняет один водитель (Employee с ролью DRIVER)
 * - Использует один автомобиль (Vehicle)
 * - Содержит множество контрольных точек маршрута (RoutePoint)
 * - Имеет одну товарно-транспортную накладную (TTN)
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_deliveryTask")
@EqualsAndHashCode(of = "id")
public class DeliveryTask {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Статус выполнения задачи
     * Отражает текущее состояние доставки:
     * PENDING - ожидает выполнения (создана, но водитель еще не начал)
     * LOADING - идет погрузка товара
     * IN_TRANSIT - в пути (водитель доставляет товар)
     * DELIVERED - доставлено (товар передан клиенту)
     * CANCELLED - отменено
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryTaskStatus status;

    /**
     * Планируемое время начала доставки
     * Устанавливается заведующим складом при создании задачи
     * Ориентир для водителя когда выезжать
     */
    @Column
    private LocalDateTime plannedStartTime;

    /**
     * Фактическое время начала доставки
     * Фиксируется автоматически когда водитель нажимает "Начать" в приложении
     */
    @Column
    private LocalDateTime actualStartTime;

    /**
     * Планируемое время окончания доставки
     * Расчетное время с учетом маршрута и контрольных точек
     */
    @Column
    private LocalDateTime plannedEndTime;

    /**
     * Фактическое время окончания доставки
     * Фиксируется когда водитель завершает задачу (статус → DELIVERED)
     */
    @Column
    private LocalDateTime actualEndTime;

    /**
     * Начальный пробег автомобиля (км)
     * Вводится водителем при начале доставки
     * Используется для расчета километража поездки
     */
    @Column
    private Integer startMileage;

    /**
     * Конечный пробег автомобиля (км)
     * Вводится водителем при завершении доставки
     * Разница (endMileage - startMileage) = километраж поездки
     */
    @Column
    private Integer endMileage;

    /**
     * Текущая широта местоположения водителя
     * Обновляется через мобильное приложение каждые 5 минут
     * Используется для отслеживания движения по маршруту
     */
    @Column
    private Double currentLatitude;

    /**
     * Текущая долгота местоположения водителя
     * Обновляется вместе с currentLatitude
     * Пара (currentLatitude, currentLongitude) = GPS координаты
     */
    @Column
    private Double currentLongitude;

    /**
     * Номер товарно-транспортной накладной
     * Уникальный номер ТТН, оформляемой для данной доставки
     * Согласно ТЗ, ТТН выдается в 2-х экземплярах
     */
    @Column(length = 50)
    private String ttnNumber;

    /**
     * Заказ клиента, который доставляется
     * Содержит информацию о товарах, адресе доставки, клиенте
     */
    @ToString.Exclude
    @OneToOne
    @JoinColumn(nullable = false)
    private ClientOrder clientOrder;

    /**
     * Водитель-экспедитор, выполняющий доставку
     * Сотрудник с ролью DRIVER
     * Отвечает за доставку товара, отчитывается заведующему складом
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Employee driver;

    /**
     * Автомобиль, используемый для доставки
     * Должен быть в статусе AVAILABLE на момент назначения
     * При начале доставки переходит в статус IN_USE
     * Тип автомобиля (обычный/рефрижератор) должен соответствовать типу товара
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Vehicle vehicle;

    /**
     * Контрольные точки маршрута
     * Включают точку отправления (склад), промежуточные контрольные точки
     * и точку назначения (адрес доставки клиента)
     * Согласно ТЗ, в путевом листе вносится информация о конечном пробеге
     */
    @ToString.Exclude
    @OneToMany(mappedBy = "deliveryTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoutePoint> routePoints = new ArrayList<>();

    /**
     * Товарно-транспортная накладная
     * Официальный документ, сопровождающий груз
     * Оформляется бухгалтером согласно ТЗ
     */
    @ToString.Exclude
    @OneToOne(mappedBy = "deliveryTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private TTN ttn;

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создает новую задачу на доставку
     * Автоматически устанавливает начальный статус PENDING
     *
     * @param clientOrder заказ клиента, который нужно доставить
     * @param driver водитель-экспедитор, назначенный на доставку
     * @param vehicle автомобиль, используемый для доставки
     */
    public DeliveryTask(ClientOrder clientOrder, Employee driver, Vehicle vehicle) {
        this.clientOrder = clientOrder;
        this.driver = driver;
        this.vehicle = vehicle;
        this.status = DeliveryTaskStatus.PENDING;
    }

    // ========== МЕТОДЫ ==========

    /**
     * Добавляет контрольную точку в маршрут
     * Устанавливает двустороннюю связь между задачей и точкой маршрута
     * Точки добавляются в порядке следования (orderIndex)
     *
     * @param routePoint контрольная точка маршрута с GPS координатами
     */
    public void addRoutePoint(RoutePoint routePoint) {
        this.routePoints.add(routePoint);
        routePoint.setDeliveryTask(this);
    }

    /**
     * Вычисляет общий пробег поездки
     * Рассчитывается как разница между конечным и начальным пробегом
     *
     * @return километраж поездки или null, если пробег еще не введен
     */
    @Transient
    public Integer getTotalMileage() {
        if (startMileage != null && endMileage != null) {
            return endMileage - startMileage;
        }
        return null;
    }
}