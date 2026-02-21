package com.mai.siarsp.repo;

import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByArticle(String article);

    boolean existsByArticleAndIdNot(String article, Long id);

    boolean existsByCategory(ProductCategory category);

    Optional<Product> findByArticle(String article);

    /**
     * Находит все товары, принадлежащие указанным категориям.
     * Используется при создании атрибута для определения товаров,
     * которым нужно задать значение нового атрибута.
     */
    @Query("SELECT p FROM Product p WHERE p.category.id IN :categoryIds")
    List<Product> findAllByCategoryIdIn(@Param("categoryIds") List<Long> categoryIds);

    List<Product> findByQuantityForStockGreaterThan(int qty);

    List<Product> findByQuantityForStockLessThan(int qty);

    Page<Product> findByQuantityForStockGreaterThan(int qty, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndQuantityForStockGreaterThan(
            String name, int qty, Pageable pageable);
}
