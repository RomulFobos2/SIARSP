package com.mai.siarsp.repo;

import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByArticle(String article);

    boolean existsByCategory(ProductCategory category);

    Optional<Product> findByArticle(String article);
}
