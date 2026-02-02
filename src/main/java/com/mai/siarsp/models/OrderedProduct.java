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

/**
 * Позиция в заказе клиента
 *
 * Представляет один товар в составе заказа клиента (ClientOrder).
 * Хранит информацию о заказанном товаре, количестве, цене на момент заказа
 * и общей стоимости позиции.
 *
 * Согласно ТЗ, в данных заказа конечного потребителя указывается:
 * - Перечень заказа (список товаров)
 * - Количество каждого товара
 * - Сумма заказа
 *
 * Особенности:
 * - Цена фиксируется на момент создания заказа (может отличаться от текущей)
 * - totalPrice рассчитывается как price × quantity
 * - Используется для формирования документов (договор, ТТН, акт)
 *
 * Связи:
 * - Каждая позиция принадлежит одному заказу (ClientOrder)
 * - Каждая позиция ссылается на один товар (Product)
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_orderedProduct")
@EqualsAndHashCode(of = "id")
public class OrderedProduct {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Заказанное количество товара
     * Количество единиц товара, которое клиент хочет получить
     *
     * Используется для:
     * - Резервирования товара на складе
     * - Расчета общей стоимости позиции
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * Цена за единицу товара на момент заказа (в рублях)
     * Фиксируется при создании заказа и не изменяется
     * даже если текущая цена товара изменится
     *
     * Это важно для:
     * - Соблюдения условий договора с клиентом
     * - Корректного финансового учета
     * - Формирования документов с согласованными ценами
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Общая стоимость позиции (в рублях)
     * Вычисляется как price × quantity
     * Автоматически пересчитывается методом recalculateTotalPrice()
     *
     * Используется для:
     * - Расчета итоговой суммы заказа (ClientOrder.totalAmount)
     * - Формирования финансовых документов
     * - Отчетности по реализации товара (ТЗ п.169-171)
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    /**
     * Товар, который заказан
     * Ссылка на справочник товаров
     * Содержит информацию о наименовании, категории, атрибутах
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Product product;

    /**
     * Заказ клиента, к которому относится позиция
     * Одна позиция принадлежит только одному заказу
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private ClientOrder clientOrder;

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создает новую позицию заказа
     * Автоматически вычисляет общую стоимость (totalPrice = price × quantity)
     *
     * @param product заказываемый товар
     * @param quantity количество единиц товара
     * @param price цена за единицу на момент заказа
     */
    public OrderedProduct(Product product, int quantity, BigDecimal price) {
        this.product = product;
        this.quantity = quantity;
        this.price = price;
        this.totalPrice = price.multiply(BigDecimal.valueOf(quantity));
    }

    // ========== МЕТОДЫ ==========

    /**
     * Пересчитывает общую стоимость позиции
     * Вызывается после изменения количества или цены
     *
     * Формула: totalPrice = price × quantity
     *
     * Используется при:
     * - Изменении количества товара в заказе
     * - Корректировке цены (до отправки заказа)
     */
    public void recalculateTotalPrice() {
        this.totalPrice = price.multiply(BigDecimal.valueOf(quantity));
    }
}