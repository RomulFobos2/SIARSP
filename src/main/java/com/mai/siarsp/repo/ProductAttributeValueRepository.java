package com.mai.siarsp.repo;

import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.ProductAttribute;
import com.mai.siarsp.models.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, Long> {

    boolean existsByProductAndAttribute(Product product, ProductAttribute attribute);

    Optional<ProductAttributeValue> findByProductAndAttribute(Product product, ProductAttribute attribute);
}
