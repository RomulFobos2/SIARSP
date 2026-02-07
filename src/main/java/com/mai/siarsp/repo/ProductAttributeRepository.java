package com.mai.siarsp.repo;

import com.mai.siarsp.models.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {

    boolean existsByName(String name);

    Optional<ProductAttribute> findByName(String name);
}
