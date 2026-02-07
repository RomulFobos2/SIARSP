package com.mai.siarsp.repo;

import com.mai.siarsp.models.Delivery;
import com.mai.siarsp.models.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    boolean existsBySupplierAndDeliveryDate(Supplier supplier, LocalDate deliveryDate);

    Optional<Delivery> findBySupplierAndDeliveryDate(Supplier supplier, LocalDate deliveryDate);
}
