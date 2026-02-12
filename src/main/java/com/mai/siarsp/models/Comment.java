package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Комментарий к заявке на поставку
 *
 * Позволяет участникам workflow (заведующий складом, директор, бухгалтер)
 * оставлять комментарии при согласовании/отклонении заявки.
 *
 * Связи:
 * - Автор комментария (Employee) — кто оставил комментарий
 * - Заявка на поставку (RequestForDelivery) — к какой заявке относится
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

    /**
     * Автор комментария — сотрудник, оставивший комментарий
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Employee author;

    /**
     * Текст комментария (до 1000 символов)
     */
    @Column(nullable = false, length = 1000)
    private String text;

    /**
     * Дата и время создания комментария
     */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Заявка на поставку, к которой относится комментарий
     */
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
