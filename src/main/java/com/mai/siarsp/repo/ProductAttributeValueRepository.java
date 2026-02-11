package com.mai.siarsp.repo;

import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.ProductAttribute;
import com.mai.siarsp.models.ProductAttributeValue;
import com.mai.siarsp.models.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, Long> {

    boolean existsByProductAndAttribute(Product product, ProductAttribute attribute);

    boolean existsByAttribute(ProductAttribute attribute);

    Optional<ProductAttributeValue> findByProductAndAttribute(Product product, ProductAttribute attribute);

    /**
     * Удаляет все значения атрибута для товаров указанной категории.
     * Используется при отвязке атрибута от категории.
     */
    @Modifying
    @Query("DELETE FROM ProductAttributeValue pav WHERE pav.product.category = :category AND pav.attribute = :attribute")
    void deleteByProductCategoryAndAttribute(@Param("category") ProductCategory category,
                                              @Param("attribute") ProductAttribute attribute);
}
