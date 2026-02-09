package com.mai.siarsp.service.employee.warehouseManager;

import com.mai.siarsp.dto.ProductCategoryDTO;
import com.mai.siarsp.mapper.ProductCategoryMapper;
import com.mai.siarsp.models.GlobalProductCategory;
import com.mai.siarsp.models.ProductCategory;
import com.mai.siarsp.repo.GlobalProductCategoryRepository;
import com.mai.siarsp.repo.ProductAttributeRepository;
import com.mai.siarsp.repo.ProductCategoryRepository;
import com.mai.siarsp.repo.ProductRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления категориями товаров.
 *
 * Обеспечивает CRUD-операции для подкатегорий товаров
 * (например: "Молоко", "Кефир" в глобальной категории "Молочная продукция").
 *
 * Составная уникальность: (globalProductCategory + name).
 * Защита удаления: категория не может быть удалена,
 * если к ней привязаны товары (Product).
 */
@Service
@Getter
@Slf4j
public class ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;
    private final GlobalProductCategoryRepository globalProductCategoryRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final ProductRepository productRepository;

    public ProductCategoryService(ProductCategoryRepository productCategoryRepository,
                                   GlobalProductCategoryRepository globalProductCategoryRepository,
                                   ProductAttributeRepository productAttributeRepository,
                                   ProductRepository productRepository) {
        this.productCategoryRepository = productCategoryRepository;
        this.globalProductCategoryRepository = globalProductCategoryRepository;
        this.productAttributeRepository = productAttributeRepository;
        this.productRepository = productRepository;
    }

    /**
     * Проверка составной уникальности (globalProductCategory + name).
     *
     * @param name                    название категории
     * @param globalProductCategoryId ID глобальной категории
     * @param id                      ID текущей категории (для исключения при редактировании), может быть null
     * @return true если категория с таким названием уже существует в данной глобальной категории
     */
    public boolean checkName(String name, Long globalProductCategoryId, Long id) {
        if (name == null || name.isBlank() || globalProductCategoryId == null) {
            return false;
        }

        Optional<GlobalProductCategory> globalCategoryOptional = globalProductCategoryRepository.findById(globalProductCategoryId);
        if (globalCategoryOptional.isEmpty()) {
            return false;
        }

        GlobalProductCategory globalCategory = globalCategoryOptional.get();

        if (id != null) {
            return productCategoryRepository.existsByGlobalProductCategoryAndNameAndIdNot(globalCategory, name, id);
        } else {
            return productCategoryRepository.existsByGlobalProductCategoryAndName(globalCategory, name);
        }
    }

    /**
     * Сохранение новой категории товара.
     *
     * @param category сущность категории для сохранения
     * @return true при успешном сохранении
     */
    @Transactional
    public boolean saveProductCategory(ProductCategory category) {
        log.info("Начинаем сохранение категории товара с названием = {}...", category.getName());

        if (checkName(category.getName(), category.getGlobalProductCategory().getId(), null)) {
            log.error("Категория с названием = {} уже существует в глобальной категории {}.",
                    category.getName(), category.getGlobalProductCategory().getName());
            return false;
        }

        try {
            productCategoryRepository.save(category);
        } catch (Exception e) {
            log.error("Ошибка при сохранении категории {}: {}", category.getName(), e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Категория товара {} успешно сохранена.", category.getName());
        return true;
    }

    /**
     * Редактирование существующей категории товара.
     *
     * @param id                      ID редактируемой категории
     * @param inputName               новое название
     * @param globalProductCategoryId ID глобальной категории
     * @param attributeIds            список ID выбранных атрибутов (может быть null)
     * @return true при успешном сохранении изменений
     */
    @Transactional
    public boolean editProductCategory(Long id, String inputName, Long globalProductCategoryId, List<Long> attributeIds) {
        Optional<ProductCategory> categoryOptional = productCategoryRepository.findById(id);

        if (categoryOptional.isEmpty()) {
            log.error("Не найдена категория товара с id = {}.", id);
            return false;
        }

        Optional<GlobalProductCategory> globalCategoryOptional = globalProductCategoryRepository.findById(globalProductCategoryId);
        if (globalCategoryOptional.isEmpty()) {
            log.error("Не найдена глобальная категория с id = {}.", globalProductCategoryId);
            return false;
        }

        if (checkName(inputName, globalProductCategoryId, id)) {
            log.error("Категория с названием = {} уже существует в глобальной категории.", inputName);
            return false;
        }

        ProductCategory category = categoryOptional.get();
        log.info("Начинаем редактирование категории товара с id = {}...", id);

        category.setName(inputName);
        category.setGlobalProductCategory(globalCategoryOptional.get());

        if (attributeIds != null && !attributeIds.isEmpty()) {
            category.setAttributes(productAttributeRepository.findAllById(attributeIds));
        } else {
            category.setAttributes(new ArrayList<>());
        }

        try {
            productCategoryRepository.save(category);
        } catch (Exception e) {
            log.error("Ошибка при сохранении изменений категории товара: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Изменения категории товара успешно сохранены.");
        return true;
    }

    /**
     * Удаление категории товара.
     * Запрещено при наличии связанных товаров (Product).
     *
     * @param id ID удаляемой категории
     * @return true при успешном удалении
     */
    @Transactional
    public boolean deleteProductCategory(Long id) {
        Optional<ProductCategory> categoryOptional = productCategoryRepository.findById(id);

        if (categoryOptional.isEmpty()) {
            log.error("Не найдена категория товара с id = {}.", id);
            return false;
        }

        ProductCategory category = categoryOptional.get();

        if (productRepository.existsByCategory(category)) {
            log.error("Невозможно удалить категорию {} — имеются связанные товары.",
                    category.getName());
            return false;
        }

        log.info("Начинаем удаление категории товара {}...", category.getName());

        try {
            productCategoryRepository.delete(category);
        } catch (Exception e) {
            log.error("Ошибка при удалении категории товара: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Категория товара успешно удалена.");
        return true;
    }

    /**
     * Получение списка всех категорий товаров в формате DTO.
     * Требует @Transactional из-за LAZY-загрузки @ManyToMany attributes.
     *
     * @return список ProductCategoryDTO
     */
    @Transactional
    public List<ProductCategoryDTO> getAllProductCategories() {
        List<ProductCategory> categories = productCategoryRepository.findAll();
        return ProductCategoryMapper.INSTANCE.toDTOList(categories);
    }
}
