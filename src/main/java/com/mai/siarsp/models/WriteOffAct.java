package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.WriteOffActStatus;
import com.mai.siarsp.enumeration.WriteOffReason;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

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