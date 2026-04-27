package com.mai.siarsp.repo;

import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.models.OrderedProduct;
import com.mai.siarsp.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderedProductRepository extends JpaRepository<OrderedProduct, Long> {

    boolean existsByClientOrderAndProduct(ClientOrder clientOrder, Product product);

    Optional<OrderedProduct> findByClientOrderAndProduct(ClientOrder clientOrder, Product product);

    long countByProductId(Long productId);

    List<OrderedProduct> findByProductIdOrderByClientOrder_OrderDateDesc(Long productId);
}
