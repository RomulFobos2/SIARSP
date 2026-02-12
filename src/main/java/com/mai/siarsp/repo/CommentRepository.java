package com.mai.siarsp.repo;

import com.mai.siarsp.models.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByRequestForDeliveryIdOrderByCreatedAtAsc(Long requestForDeliveryId);
}
