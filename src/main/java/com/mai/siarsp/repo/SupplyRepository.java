package com.mai.siarsp.repo;

import com.mai.siarsp.models.Delivery;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.Supply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplyRepository extends JpaRepository<Supply, Long> {

    boolean existsByDeliveryAndProduct(Delivery delivery, Product product);

    Optional<Supply> findByDeliveryAndProduct(Delivery delivery, Product product);

    long countByProductId(Long productId);

    List<Supply> findByProductIdOrderByDelivery_DeliveryDateDesc(Long productId);
}
