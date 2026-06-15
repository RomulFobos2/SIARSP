package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.WriteOffActStatus;
import com.mai.siarsp.enumeration.WriteOffReason;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Акт списания товара (брак, порча, истечение срока). Закрывает складскую операцию и оставляет аудитный след.
 */

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_writeOffAct")
@EqualsAndHashCode(of = "id")
public class WriteOffAct {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String actNumber;

    @Column(nullable = false)
    private LocalDate actDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WriteOffReason reason;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WriteOffActStatus status = WriteOffActStatus.PENDING_DIRECTOR;

    @Column(length = 500)
    private String comment;

    @Column(length = 500)
    private String directorComment;

    /**
     * Стоимость списанного товара на момент создания акта = quantity × последняя закупочная цена.
     * Хранится как snapshot для аудита: будущие закупки не меняют историю.
     * null — если у товара нет ни одной закупки.
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal totalCost;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Product product;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Employee responsibleEmployee;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = true)
    private Warehouse warehouse;

    /**
     * Партия товара, к которой относится списание. Nullable для совместимости со старыми
     * актами (до миграции на партионный учёт). Для новых актов рекомендуется всегда задавать.
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "supply_id")
    private Supply supply;

    /**
     * Зона хранения для адресного списания: списывается ровно одна запись ZoneProduct(zone, supply).
     * Nullable: автосписание просрочки оставляет zone=null и распределяет списание по всем зонам партии
     * на складе. Для ручных актов с июня 2026 задаётся всегда.
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "zone_id")
    private StorageZone zone;

    // ========== КОНСТРУКТОРЫ ==========

    public WriteOffAct(String actNumber, Product product, int quantity,
                       WriteOffReason reason, Employee responsibleEmployee) {
        this.actNumber = actNumber;
        this.product = product;
        this.quantity = quantity;
        this.reason = reason;
        this.responsibleEmployee = responsibleEmployee;
        this.actDate = LocalDate.now();
    }

    // ========== МЕТОДЫ ==========
    // Специфичных методов нет, используются стандартные getter/setter от Lombok
    //
    // Возможные методы для добавления в сервисный слой:
    // - calculateLossAmount() - расчет суммы убытка (quantity × закупочная_цена)
    // - approve() - утверждение акта и списание товара со склада
    // - generateActDocument() - формирование печатной формы акта
    // - validateQuantity() - проверка достаточности товара для списания

}