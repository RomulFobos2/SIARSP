package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.ClientOrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Заказ клиента
 *
 * Центральная сущность бизнес-процесса обработки заказов от конечных потребителей.
 * Создается директором после переговоров с клиентом. Содержит информацию о заказанных товарах,
 * датах, статусе выполнения. Проходит через весь жизненный цикл: создание → резервирование →
 * комплектация → отгрузка → доставка.
 *
 * Связи:
 * - Один заказ принадлежит одному клиенту (Client)
 * - Один заказ имеет множество позиций товаров (OrderedProduct)
 * - Один заказ может иметь одну задачу на доставку (DeliveryTask)
 * - Один заказ может иметь один приемо-сдаточный акт (AcceptanceAct)
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_clientOrder")
@EqualsAndHashCode(of = "id")
public class ClientOrder {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Номер заказа
     * Уникальный номер, генерируется автоматически при создании
     * Формат: ЗК-YYYYMMDD-NNNN (например, ЗК-20250203-0042)
     * Используется в документах и для идентификации заказа
     */
    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    /**
     * Дата и время создания заказа
     * Фиксируется автоматически при создании заказа директором
     */
    @Column(nullable = false)
    private LocalDateTime orderDate;

    /**
     * Планируемая дата доставки
     * Согласовывается с клиентом при создании заказа
     * Используется для планирования работы склада и логистики
     */
    @Column(nullable = false)
    private LocalDate deliveryDate;

    /**
     * Фактическая дата доставки
     * Заполняется после выполнения доставки водителем-экспедитором
     * Может отличаться от планируемой даты
     */
    @Column
    private LocalDate actualDeliveryDate;

    /**
     * Статус заказа
     * Отражает текущее состояние заказа в жизненном цикле:
     * NEW → CONFIRMED → RESERVED → IN_PROGRESS → READY → SHIPPED → DELIVERED
     * Также может быть CANCELLED (отменен)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientOrderStatus status;

    /**
     * Общая сумма заказа (в рублях)
     * Вычисляется автоматически методом calculateTotalAmount()
     * как сумма всех позиций заказа (OrderedProduct.totalPrice)
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Комментарий к заказу
     * Может содержать особые пожелания клиента, примечания,
     * условия доставки и т.п.
     */
    @Column(length = 500)
    private String comment;

    /**
     * Путь к файлу договора на отгрузку
     * Оформляется бухгалтером согласно ТЗ
     * Хранится путь к файлу в файловой системе
     */
    @Column(length = 500)
    private String contractFile;

    /**
     * Клиент, разместивший заказ
     * Конечный потребитель (детский сад, школа, больница)
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Client client;

    /**
     * Ответственный сотрудник
     * Сотрудник (обычно директор), который создал и ведет заказ
     * Согласно ТЗ, директор фиксирует основные сведения о заказе
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Employee responsibleEmployee;

    /**
     * Список позиций заказа (заказанных товаров)
     * Каждая позиция содержит товар, количество и цену
     */
    @ToString.Exclude
    @OneToMany(mappedBy = "clientOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderedProduct> orderedProducts = new ArrayList<>();

    /**
     * Задача на доставку
     * Создается заведующим складом для водителя-экспедитора
     * Содержит маршрут, контрольные точки, информацию о транспорте
     */
    @ToString.Exclude
    @OneToOne(mappedBy = "clientOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private DeliveryTask deliveryTask;

    /**
     * Приемо-сдаточный акт
     * Оформляется при доставке товара клиенту
     * Подписывается представителем клиента и водителем
     */
    @ToString.Exclude
    @OneToOne(mappedBy = "clientOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private AcceptanceAct acceptanceAct;

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создает новый заказ клиента
     * Автоматически устанавливает дату создания, начальный статус (NEW)
     * и обнуляет общую сумму (будет рассчитана позже)
     *
     * @param orderNumber номер заказа (генерируется сервисом)
     * @param client клиент, разместивший заказ
     * @param responsibleEmployee ответственный сотрудник (обычно директор)
     * @param deliveryDate планируемая дата доставки
     */
    public ClientOrder(String orderNumber, Client client, Employee responsibleEmployee, LocalDate deliveryDate) {
        this.orderNumber = orderNumber;
        this.client = client;
        this.responsibleEmployee = responsibleEmployee;
        this.deliveryDate = deliveryDate;
        this.orderDate = LocalDateTime.now();
        this.status = ClientOrderStatus.NEW;
        this.totalAmount = BigDecimal.ZERO;
    }

    // ========== МЕТОДЫ ==========

    /**
     * Добавляет позицию товара в заказ
     * Устанавливает двустороннюю связь между заказом и позицией
     *
     * @param orderedProduct позиция заказа (товар + количество + цена)
     */
    public void addOrderedProduct(OrderedProduct orderedProduct) {
        this.orderedProducts.add(orderedProduct);
        orderedProduct.setClientOrder(this);
    }

    /**
     * Вычисляет общую сумму заказа
     * Суммирует стоимость всех позиций заказа (OrderedProduct.totalPrice)
     * Результат сохраняется в поле totalAmount
     *
     * Вызывается после добавления всех позиций заказа или при изменении цен
     */
    public void calculateTotalAmount() {
        this.totalAmount = orderedProducts.stream()
                .map(OrderedProduct::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}