package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.WriteOffReason;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

//TODO Пока оставлю, но скорее всего уберу. Акты будут просто формироваться на основе шаблона ворд документа и как файл хранится в хранилище, а в заказе будет поле на шаблон этого акта.
//TODO нужно подумать грузить пдф подписанный или что? ну разберемся
/**
 * Финальный документ по заказу: фиксирует факт передачи товара клиенту, подписи сторон и итоговые замечания по доставке.
 */

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_acceptanceAct")
@EqualsAndHashCode(of = "id")
public class AcceptanceAct {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String actNumber;

    @Column(nullable = false)
    private LocalDate actDate;

    @Column(length = 200)
    private String clientRepresentative;

    @Column(nullable = false)
    private boolean signed;

    @Column
    private LocalDateTime signedAt;

    @Column(length = 500)
    private String comment;

    @ToString.Exclude
    @OneToOne
    @JoinColumn(nullable = false)
    private ClientOrder clientOrder;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Client client;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Employee deliveredBy;

    // ========== КОНСТРУКТОРЫ ==========

    public AcceptanceAct(String actNumber, ClientOrder clientOrder,
                         Client client, Employee deliveredBy) {
        this.actNumber = actNumber;
        this.clientOrder = clientOrder;
        this.client = client;
        this.deliveredBy = deliveredBy;
        this.actDate = LocalDate.now();
        this.signed = false;
    }

    // ========== МЕТОДЫ ==========

    public void markAsSigned(String clientRepresentative) {
        this.signed = true;
        this.signedAt = LocalDateTime.now();
        this.clientRepresentative = clientRepresentative;
    }

}