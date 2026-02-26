package com.mai.siarsp.repo;

import com.mai.siarsp.enumeration.RequestStatus;
import com.mai.siarsp.models.RequestForDelivery;
import com.mai.siarsp.models.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RequestForDeliveryRepository extends JpaRepository<RequestForDelivery, Long> {

    boolean existsBySupplierAndStatus(Supplier supplier, RequestStatus status);

    Optional<RequestForDelivery> findBySupplierAndStatus(Supplier supplier, RequestStatus status);

    List<RequestForDelivery> findAllByOrderByRequestDateDesc();

    List<RequestForDelivery> findByStatusOrderByRequestDateDesc(RequestStatus status);

    List<RequestForDelivery> findByStatusInOrderByRequestDateDesc(List<RequestStatus> statuses);

    /**
     * Среднее время поставки (в днях) по каждому товару
     * на основе завершённых заявок (статус RECEIVED)
     */
    @Query(value = "SELECT rp.product_id, AVG(DATEDIFF(r.received_date, r.request_date)) " +
            "FROM t_requestForDelivery r " +
            "JOIN t_requestedProduct rp ON rp.request_id = r.id " +
            "WHERE r.status = 'RECEIVED' AND r.received_date IS NOT NULL " +
            "GROUP BY rp.product_id", nativeQuery = true)
    List<Object[]> findAverageDeliveryDaysByProduct();
}
