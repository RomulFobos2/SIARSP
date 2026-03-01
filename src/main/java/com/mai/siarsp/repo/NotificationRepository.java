package com.mai.siarsp.repo;

import com.mai.siarsp.enumeration.NotificationStatus;
import com.mai.siarsp.models.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdAndVisibleTrueOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findByRecipientIdAndVisibleTrueAndStatusOrderByCreatedAtDesc(Long recipientId, NotificationStatus status);

    long countByRecipientIdAndStatusAndVisibleTrue(Long recipientId, NotificationStatus status);

    // Пагинированные методы
    Page<Notification> findByRecipientIdAndVisibleTrueOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndVisibleTrueAndStatusOrderByCreatedAtDesc(Long recipientId, NotificationStatus status, Pageable pageable);

    Page<Notification> findByRecipientIdAndVisibleTrueAndTextContainingIgnoreCaseOrderByCreatedAtDesc(Long recipientId, String text, Pageable pageable);

    Page<Notification> findByRecipientIdAndVisibleTrueAndStatusAndTextContainingIgnoreCaseOrderByCreatedAtDesc(Long recipientId, NotificationStatus status, String text, Pageable pageable);
}
