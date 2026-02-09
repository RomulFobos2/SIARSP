package com.mai.siarsp.repo;

import com.mai.siarsp.models.GlobalProductCategory;
import com.mai.siarsp.models.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    boolean existsByGlobalProductCategoryAndName(GlobalProductCategory globalProductCategory, String name);

    boolean existsByGlobalProductCategory(GlobalProductCategory globalProductCategory);

    Optional<ProductCategory> findByGlobalProductCategoryAndName(GlobalProductCategory globalProductCategory, String name);
}
