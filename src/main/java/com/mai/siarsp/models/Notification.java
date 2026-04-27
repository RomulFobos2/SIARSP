package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.NotificationStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Системное уведомление для сотрудников о важных событиях: новая задача, смена статуса заказа, просрочки и т.д.
 */

@Entity
@Table(name = "t_notification")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Notification {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Employee recipient;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.NEW;

    @Column(nullable = false)
    private boolean visible = true;

    // ========== КОНСТРУКТОРЫ ==========

    public Notification(Employee recipient, String text) {
        this.recipient = recipient;
        this.text = text;
        this.createdAt = LocalDateTime.now();
        this.status = NotificationStatus.NEW;
        this.visible = true;
    }
}
