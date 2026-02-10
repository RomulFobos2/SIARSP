package com.mai.siarsp.service.employee.manager;

import com.mai.siarsp.dto.ProductDTO;
import com.mai.siarsp.mapper.ProductMapper;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.ProductCategory;
import com.mai.siarsp.repo.ProductCategoryRepository;
import com.mai.siarsp.repo.ProductRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления товарами (Product) со стороны руководителя
 *
 * Предоставляет операции проверки уникальности артикула, создания,
 * редактирования, удаления и получения списка товаров.
 */
@Service
@Getter
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;

    public ProductService(ProductRepository productRepository,
                          ProductCategoryRepository productCategoryRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
    }

    /**
     * Проверяет занятость артикула товара.
     *
     * @param article артикул для проверки
     * @param id ID текущего товара (null при создании)
     * @return true если артикул уже используется
     */
    public boolean checkArticle(String article, Long id) {
        if (article == null || article.isBlank()) {
            return false;
        }

        if (id != null) {
            return productRepository.existsByArticleAndIdNot(article, id);
        }

        return productRepository.existsByArticle(article);
    }

    /**
     * Сохраняет новый товар с привязкой к категории.
     *
     * @param product объект товара для сохранения
     * @param categoryId ID категории товара
     * @return true при успешном сохранении
     */
    @Transactional
    public boolean saveProduct(Product product, Long categoryId) {
        if (checkArticle(product.getArticle(), null)) {
            log.error("Товар с артикулом {} уже существует.", product.getArticle());
            return false;
        }

        Optional<ProductCategory> categoryOptional = productCategoryRepository.findById(categoryId);
        if (categoryOptional.isEmpty()) {
            log.error("Категория с id = {} не найдена.", categoryId);
            return false;
        }

        product.setCategory(categoryOptional.get());

        try {
            productRepository.save(product);
        } catch (Exception e) {
            log.error("Ошибка при сохранении товара: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        return true;
    }

    /**
     * Обновляет данные существующего товара.
     *
     * @param id ID редактируемого товара
     * @param inputName новое название
     * @param inputArticle новый артикул
     * @param inputStockQuantity новое количество на складе
     * @param inputQuantityForStock новое количество к размещению
     * @param inputImage новый путь к изображению
     * @param inputWarehouseType новый тип склада
     * @param inputCategoryId новая категория
     * @return true при успешном обновлении
     */
    @Transactional
    public boolean editProduct(Long id,
                               String inputName,
                               String inputArticle,
                               int inputStockQuantity,
                               int inputQuantityForStock,
                               String inputImage,
                               com.mai.siarsp.enumeration.WarehouseType inputWarehouseType,
                               Long inputCategoryId) {
        Optional<Product> productOptional = productRepository.findById(id);
        if (productOptional.isEmpty()) {
            log.error("Товар с id = {} не найден.", id);
            return false;
        }

        if (checkArticle(inputArticle, id)) {
            log.error("Товар с артикулом {} уже существует.", inputArticle);
            return false;
        }

        Optional<ProductCategory> categoryOptional = productCategoryRepository.findById(inputCategoryId);
        if (categoryOptional.isEmpty()) {
            log.error("Категория с id = {} не найдена.", inputCategoryId);
            return false;
        }

        Product product = productOptional.get();
        product.setName(inputName);
        product.setArticle(inputArticle);
        product.setStockQuantity(inputStockQuantity);
        product.setQuantityForStock(inputQuantityForStock);
        product.setImage(inputImage);
        product.setWarehouseType(inputWarehouseType);
        product.setCategory(categoryOptional.get());

        try {
            productRepository.save(product);
        } catch (Exception e) {
            log.error("Ошибка при обновлении товара: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        return true;
    }

    /**
     * Удаляет товар по идентификатору.
     *
     * @param id ID товара
     * @return true при успешном удалении
     */
    @Transactional
    public boolean deleteProduct(Long id) {
        Optional<Product> productOptional = productRepository.findById(id);

        if (productOptional.isEmpty()) {
            log.error("Товар с id = {} не найден.", id);
            return false;
        }

        try {
            productRepository.delete(productOptional.get());
        } catch (Exception e) {
            log.error("Ошибка при удалении товара: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        return true;
    }

    /**
     * Возвращает список всех товаров в формате DTO.
     *
     * @return список ProductDTO
     */
    @Transactional
    public List<ProductDTO> getAllProducts() {
        return ProductMapper.INSTANCE.toDTOList(productRepository.findAll());
    }
}
