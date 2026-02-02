package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Поставка товара от поставщика
 *
 * Общая информация о поставке товаров от поставщика на склад ИП "Левчук".
 * Создается после согласования заявки на поставку (RequestForDelivery) директором и бухгалтером.
 * Содержит список конкретных товаров (Supply) с ценами и количествами.
 * Является основанием для оприходования товара на склад.
 *
 * Бизнес-процесс:
 * 1. Заведующий складом создает RequestForDelivery
 * 2. Директор и бухгалтер согласовывают
 * 3. Поставщик привозит товар → создается Delivery
 * 4. Заведующий складом принимает товар, проверяет по накладной
 * 5. При успешной приемке → товар оприходуется (stockQuantity увеличивается)
 *
 * Связи:
 * - Одна поставка от одного поставщика (Supplier)
 * - Одна поставка содержит множество позиций товара (Supply)
 * - Одна поставка может быть связана с одной заявкой (RequestForDelivery)
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_delivery")
@EqualsAndHashCode(of = "id")
public class Delivery {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Дата поставки товара
     * Фактическая дата, когда поставщик привез товар на склад
     * Используется для учета и отчетности
     */
    @Column(nullable = false)
    private LocalDate deliveryDate;

    /**
     * Поставщик товара
     * Организация, которая поставила товар
     * Связь с основными реквизитами и контактными данными поставщика
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Supplier supplier;

    /**
     * Список позиций поставки
     * Каждая позиция содержит конкретный товар, его закупочную цену и количество
     * Например: "Молоко 3.2% - 100 шт. по 45 руб."
     */
    @ToString.Exclude
    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Supply> supplies = new ArrayList<>();

    /**
     * Связь с заявкой на поставку
     * Если поставка была инициирована через заявку RequestForDelivery,
     * то здесь хранится ссылка на неё
     * Может быть null, если поставка создана напрямую
     */
    @ToString.Exclude
    @OneToOne(mappedBy = "delivery")
    private RequestForDelivery request;

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создает новую поставку от поставщика
     * Инициализирует пустой список позиций товаров
     *
     * @param supplier поставщик, от которого поступил товар
     * @param deliveryDate дата фактической поставки
     */
    public Delivery(Supplier supplier, LocalDate deliveryDate) {
        this.supplier = supplier;
        this.deliveryDate = deliveryDate;
        this.supplies = new ArrayList<>();
    }

    // ========== МЕТОДЫ ==========

    /**
     * Добавляет позицию товара в поставку
     * Устанавливает двустороннюю связь между поставкой и позицией
     *
     * @param supply позиция поставки (товар + закупочная цена + количество)
     */
    public void addSupply(Supply supply) {
        this.supplies.add(supply);
        supply.setDelivery(this);
    }

    /**
     * Вычисляет общую стоимость поставки
     * Суммирует стоимость всех позиций товара в поставке
     *
     * @return общая сумма поставки в рублях (сумма всех Supply.totalPrice)
     */
    public BigDecimal getTotalCost() {
        return supplies.stream()
                .map(Supply::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}