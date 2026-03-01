package com.mai.siarsp.repo;

import com.mai.siarsp.enumeration.DeliveryTaskStatus;
import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.models.DeliveryTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryTaskRepository extends JpaRepository<DeliveryTask, Long> {

    boolean existsByClientOrder(ClientOrder clientOrder);

    Optional<DeliveryTask> findByClientOrder(ClientOrder clientOrder);

    @Query("SELECT dt FROM DeliveryTask dt LEFT JOIN FETCH dt.routePoints " +
           "LEFT JOIN FETCH dt.clientOrder LEFT JOIN FETCH dt.driver " +
           "LEFT JOIN FETCH dt.vehicle LEFT JOIN FETCH dt.ttn WHERE dt.id = :id")
    Optional<DeliveryTask> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT dt FROM DeliveryTask dt LEFT JOIN FETCH dt.routePoints " +
           "LEFT JOIN FETCH dt.clientOrder LEFT JOIN FETCH dt.driver " +
           "LEFT JOIN FETCH dt.vehicle LEFT JOIN FETCH dt.ttn " +
           "ORDER BY dt.plannedStartTime DESC")
    List<DeliveryTask> findAllWithDetails();

    @Query("SELECT DISTINCT dt FROM DeliveryTask dt LEFT JOIN FETCH dt.routePoints " +
           "LEFT JOIN FETCH dt.clientOrder LEFT JOIN FETCH dt.driver " +
           "LEFT JOIN FETCH dt.vehicle LEFT JOIN FETCH dt.ttn " +
           "WHERE dt.status IN :statuses ORDER BY dt.plannedStartTime DESC")
    List<DeliveryTask> findByStatusInWithDetails(@Param("statuses") List<DeliveryTaskStatus> statuses);

    @Query("SELECT DISTINCT dt FROM DeliveryTask dt LEFT JOIN FETCH dt.routePoints " +
           "LEFT JOIN FETCH dt.clientOrder LEFT JOIN FETCH dt.driver " +
           "LEFT JOIN FETCH dt.vehicle LEFT JOIN FETCH dt.ttn " +
           "WHERE dt.driver.id = :driverId ORDER BY dt.plannedStartTime DESC")
    List<DeliveryTask> findByDriverIdWithDetails(@Param("driverId") Long driverId);

    @Query("SELECT DISTINCT dt FROM DeliveryTask dt LEFT JOIN FETCH dt.routePoints " +
           "LEFT JOIN FETCH dt.clientOrder LEFT JOIN FETCH dt.driver " +
           "LEFT JOIN FETCH dt.vehicle LEFT JOIN FETCH dt.ttn " +
           "WHERE dt.driver.id = :driverId AND dt.status IN :statuses " +
           "ORDER BY dt.plannedStartTime DESC")
    List<DeliveryTask> findByDriverIdAndStatusInWithDetails(
            @Param("driverId") Long driverId,
            @Param("statuses") List<DeliveryTaskStatus> statuses);

    List<DeliveryTask> findAllByOrderByPlannedStartTimeDesc();

    List<DeliveryTask> findByStatusInOrderByPlannedStartTimeDesc(List<DeliveryTaskStatus> statuses);

    List<DeliveryTask> findByDriverIdOrderByPlannedStartTimeDesc(Long driverId);

    List<DeliveryTask> findByDriverIdAndStatusInOrderByPlannedStartTimeDesc(Long driverId, List<DeliveryTaskStatus> statuses);

    @Query("SELECT DISTINCT dt FROM DeliveryTask dt LEFT JOIN FETCH dt.clientOrder co " +
           "LEFT JOIN FETCH co.client LEFT JOIN FETCH dt.driver " +
           "LEFT JOIN FETCH dt.vehicle " +
           "WHERE dt.vehicle.id = :vehicleId ORDER BY dt.plannedStartTime DESC")
    List<DeliveryTask> findByVehicleIdWithDetails(@Param("vehicleId") Long vehicleId);
}
