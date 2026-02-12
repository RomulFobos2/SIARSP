package com.mai.siarsp.repo;

import com.mai.siarsp.enumeration.RequestStatus;
import com.mai.siarsp.models.RequestForDelivery;
import com.mai.siarsp.models.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RequestForDeliveryRepository extends JpaRepository<RequestForDelivery, Long> {

    boolean existsBySupplierAndStatus(Supplier supplier, RequestStatus status);

    Optional<RequestForDelivery> findBySupplierAndStatus(Supplier supplier, RequestStatus status);

    List<RequestForDelivery> findByStatusOrderByRequestDateDesc(RequestStatus status);

    List<RequestForDelivery> findByStatusInOrderByRequestDateDesc(List<RequestStatus> statuses);

    List<RequestForDelivery> findAllByOrderByRequestDateDesc();
}
