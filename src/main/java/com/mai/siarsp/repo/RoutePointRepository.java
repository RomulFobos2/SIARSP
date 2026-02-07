package com.mai.siarsp.repo;

import com.mai.siarsp.models.DeliveryTask;
import com.mai.siarsp.models.RoutePoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoutePointRepository extends JpaRepository<RoutePoint, Long> {

    boolean existsByDeliveryTaskAndOrderIndex(DeliveryTask deliveryTask, int orderIndex);

    Optional<RoutePoint> findByDeliveryTaskAndOrderIndex(DeliveryTask deliveryTask, int orderIndex);
}
