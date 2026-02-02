package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.WriteOffReason;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;


@Data
@NoArgsConstructor
@Entity
@Table(name = "t_writeOffAct")
@EqualsAndHashCode(of = "id")
//Акт списания
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

    @Column(length = 500)
    private String comment;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Product product;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Employee responsibleEmployee;

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

}
