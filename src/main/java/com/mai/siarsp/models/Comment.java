package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Внутренний комментарий сотрудников к заявке или задаче. Нужен, чтобы не терять контекст по спорным и нестандартным кейсам.
 */

@Entity
@Table(name = "t_comment")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Comment {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Employee author;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private RequestForDelivery requestForDelivery;

    // ========== КОНСТРУКТОРЫ ==========

    public Comment(Employee author, String text, RequestForDelivery requestForDelivery) {
        this.author = author;
        this.text = text;
        this.requestForDelivery = requestForDelivery;
        this.createdAt = LocalDateTime.now();
    }
}
