package com.mai.siarsp.repo;

import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.RequestForDelivery;
import com.mai.siarsp.models.RequestedProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RequestedProductRepository extends JpaRepository<RequestedProduct, Long> {

    boolean existsByRequestAndProduct(RequestForDelivery request, Product product);

    Optional<RequestedProduct> findByRequestAndProduct(RequestForDelivery request, Product product);
}
