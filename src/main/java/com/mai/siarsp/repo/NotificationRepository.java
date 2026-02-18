package com.mai.siarsp.repo;

import com.mai.siarsp.enumeration.NotificationStatus;
import com.mai.siarsp.models.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findByRecipientIdAndStatusOrderByCreatedAtDesc(Long recipientId, NotificationStatus status);

    long countByRecipientIdAndStatus(Long recipientId, NotificationStatus status);

    // Пагинированные методы
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndStatusOrderByCreatedAtDesc(Long recipientId, NotificationStatus status, Pageable pageable);

    Page<Notification> findByRecipientIdAndTextContainingIgnoreCaseOrderByCreatedAtDesc(Long recipientId, String text, Pageable pageable);

    Page<Notification> findByRecipientIdAndStatusAndTextContainingIgnoreCaseOrderByCreatedAtDesc(Long recipientId, NotificationStatus status, String text, Pageable pageable);
}
