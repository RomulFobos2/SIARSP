package com.mai.siarsp.service.employee.manager;

import com.mai.siarsp.dto.ProductDTO;
import com.mai.siarsp.enumeration.WarehouseType;
import com.mai.siarsp.mapper.ProductMapper;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.ProductCategory;
import com.mai.siarsp.repo.ProductCategoryRepository;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.service.general.ImageService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления товарами (Product) со стороны руководителя
 *
 * Предоставляет операции проверки уникальности артикула, создания,
 * редактирования, удаления и получения списка товаров.
 */
@Service("managerProductService")
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
     * Сохраняет новый товар с привязкой к категории и загрузкой изображения.
     *
     * @param product объект товара для сохранения
     * @param categoryId ID категории товара
     * @param inputFileField файл изображения товара
     * @return Optional с ID сохранённого товара или empty при ошибке
     */
    @Transactional
    public Optional<Long> saveProduct(Product product, Long categoryId, MultipartFile inputFileField) {
        if (checkArticle(product.getArticle(), null)) {
            log.error("Товар с артикулом {} уже существует.", product.getArticle());
            return Optional.empty();
        }

        Optional<ProductCategory> categoryOptional = productCategoryRepository.findById(categoryId);
        if (categoryOptional.isEmpty()) {
            log.error("Категория с id = {} не найдена.", categoryId);
            return Optional.empty();
        }

        if (inputFileField == null || inputFileField.isEmpty()) {
            log.error("Изображение товара обязательно при создании.");
            return Optional.empty();
        }

        product.setCategory(categoryOptional.get());

        try {
            product.setImage(ImageService.uploadImage(inputFileField));
            productRepository.save(product);
        } catch (Exception e) {
            log.error("Ошибка при сохранении товара: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Optional.empty();
        }

        return Optional.of(product.getId());
    }

    /**
     * Обновляет данные существующего товара и, при необходимости, изображение.
     *
     * @param id ID редактируемого товара
     * @param inputName новое название
     * @param inputArticle новый артикул
     * @param inputStockQuantity новое количество на складе
     * @param inputQuantityForStock новое количество к размещению
     * @param inputWarehouseType новый тип склада
     * @param inputCategoryId новая категория
     * @param inputFileField новый файл изображения (необязательный)
     * @return Optional с ID товара или empty при ошибке
     */
    @Transactional
    public Optional<Long> editProduct(Long id,
                                      String inputName,
                                      String inputArticle,
                                      int inputStockQuantity,
                                      int inputQuantityForStock,
                                      WarehouseType inputWarehouseType,
                                      Long inputCategoryId,
                                      MultipartFile inputFileField) {
        Optional<Product> productOptional = productRepository.findById(id);
        if (productOptional.isEmpty()) {
            log.error("Товар с id = {} не найден.", id);
            return Optional.empty();
        }

        if (checkArticle(inputArticle, id)) {
            log.error("Товар с артикулом {} уже существует.", inputArticle);
            return Optional.empty();
        }

        Optional<ProductCategory> categoryOptional = productCategoryRepository.findById(inputCategoryId);
        if (categoryOptional.isEmpty()) {
            log.error("Категория с id = {} не найдена.", inputCategoryId);
            return Optional.empty();
        }

        Product product = productOptional.get();
        product.setName(inputName);
        product.setArticle(inputArticle);
        product.setStockQuantity(inputStockQuantity);
        product.setQuantityForStock(inputQuantityForStock);
        product.setWarehouseType(inputWarehouseType);
        product.setCategory(categoryOptional.get());

        try {
            if (inputFileField != null && !inputFileField.isEmpty()) {
                product.setImage(ImageService.uploadImage(inputFileField));
            }
            productRepository.save(product);
        } catch (Exception e) {
            log.error("Ошибка при обновлении товара: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Optional.empty();
        }

        return Optional.of(product.getId());
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
