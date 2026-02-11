package com.mai.siarsp.repo;

import com.mai.siarsp.models.GlobalProductCategory;
import com.mai.siarsp.models.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    /**
     * Загружает все категории вместе с атрибутами (JOIN FETCH).
     * Используется в RoleRunner для избежания LazyInitializationException
     * при доступе к category.getAttributes() вне транзакции.
     */
    @Query("SELECT DISTINCT c FROM ProductCategory c LEFT JOIN FETCH c.attributes")
    List<ProductCategory> findAllWithAttributes();

    boolean existsByGlobalProductCategoryAndName(GlobalProductCategory globalProductCategory, String name);

    boolean existsByGlobalProductCategoryAndNameAndIdNot(GlobalProductCategory globalProductCategory, String name, Long id);

    boolean existsByGlobalProductCategory(GlobalProductCategory globalProductCategory);

    Optional<ProductCategory> findByGlobalProductCategoryAndName(GlobalProductCategory globalProductCategory, String name);
}
