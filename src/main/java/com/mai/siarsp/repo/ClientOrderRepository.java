package com.mai.siarsp.repo;

import com.mai.siarsp.models.ClientOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientOrderRepository extends JpaRepository<ClientOrder, Long> {

    boolean existsByOrderNumber(String orderNumber);

    Optional<ClientOrder> findByOrderNumber(String orderNumber);
}
