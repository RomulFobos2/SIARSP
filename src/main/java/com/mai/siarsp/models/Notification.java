package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.NotificationStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Уведомление для сотрудника
 *
 * Универсальная сущность для системных уведомлений.
 * Уведомления создаются автоматически при различных бизнес-событиях
 * (смена статуса заявки, назначение задачи и т.д.) и отображаются
 * сотруднику в интерфейсе.
 *
 * Связи:
 * - Получатель (Employee) — сотрудник, которому адресовано уведомление
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

    /**
     * Получатель уведомления
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Employee recipient;

    /**
     * Текст уведомления (до 500 символов)
     */
    @Column(nullable = false, length = 500)
    private String text;

    /**
     * Дата и время создания уведомления
     */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Статус уведомления (NEW / READ)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.NEW;

    // ========== КОНСТРУКТОРЫ ==========

    public Notification(Employee recipient, String text) {
        this.recipient = recipient;
        this.text = text;
        this.createdAt = LocalDateTime.now();
        this.status = NotificationStatus.NEW;
    }
}
