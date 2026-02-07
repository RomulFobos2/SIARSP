package com.mai.siarsp.repo;

import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.models.DeliveryTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryTaskRepository extends JpaRepository<DeliveryTask, Long> {

    boolean existsByClientOrder(ClientOrder clientOrder);

    Optional<DeliveryTask> findByClientOrder(ClientOrder clientOrder);
}
