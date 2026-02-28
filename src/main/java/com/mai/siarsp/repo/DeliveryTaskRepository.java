package com.mai.siarsp.repo;

import com.mai.siarsp.enumeration.DeliveryTaskStatus;
import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.models.DeliveryTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryTaskRepository extends JpaRepository<DeliveryTask, Long> {

    boolean existsByClientOrder(ClientOrder clientOrder);

    Optional<DeliveryTask> findByClientOrder(ClientOrder clientOrder);

    List<DeliveryTask> findAllByOrderByPlannedStartTimeDesc();

    List<DeliveryTask> findByStatusInOrderByPlannedStartTimeDesc(List<DeliveryTaskStatus> statuses);

    List<DeliveryTask> findByDriverIdOrderByPlannedStartTimeDesc(Long driverId);

    List<DeliveryTask> findByDriverIdAndStatusInOrderByPlannedStartTimeDesc(Long driverId, List<DeliveryTaskStatus> statuses);
}
