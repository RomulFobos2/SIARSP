package com.mai.siarsp.service.employee.warehouseManager;

import com.mai.siarsp.dto.ProductCategoryDTO;
import com.mai.siarsp.mapper.ProductCategoryMapper;
import com.mai.siarsp.models.GlobalProductCategory;
import com.mai.siarsp.models.ProductCategory;
import com.mai.siarsp.repo.GlobalProductCategoryRepository;
import com.mai.siarsp.models.ProductAttribute;
import com.mai.siarsp.repo.ProductAttributeRepository;
import com.mai.siarsp.repo.ProductAttributeValueRepository;
import com.mai.siarsp.repo.ProductCategoryRepository;
import com.mai.siarsp.repo.ProductRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.ProductAttributeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис категорий товара в контуре склада: структура каталога и правила классификации.
 */

@Service
@Getter
@Slf4j
public class ProductCategoryService {

    public static final List<String> MANDATORY_ATTRIBUTE_NAMES = List.of(
            "Длина упаковки", "Ширина упаковки", "Высота упаковки", "Срок годности"
    );

    private final ProductCategoryRepository productCategoryRepository;
    private final GlobalProductCategoryRepository globalProductCategoryRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final ProductRepository productRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;

    public ProductCategoryService(ProductCategoryRepository productCategoryRepository,
                                   GlobalProductCategoryRepository globalProductCategoryRepository,
                                   ProductAttributeRepository productAttributeRepository,
                                   ProductRepository productRepository,
                                   ProductAttributeValueRepository productAttributeValueRepository) {
        this.productCategoryRepository = productCategoryRepository;
        this.globalProductCategoryRepository = globalProductCategoryRepository;
        this.productAttributeRepository = productAttributeRepository;
        this.productRepository = productRepository;
        this.productAttributeValueRepository = productAttributeValueRepository;
    }

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

    public List<ProductAttribute> getMandatoryAttributes() {
        return productAttributeRepository.findAll().stream()
                .filter(attr -> MANDATORY_ATTRIBUTE_NAMES.contains(attr.getName()))
                .toList();
    }

    @Transactional
    public boolean saveProductCategory(ProductCategory category) {
        log.info("Начинаем сохранение категории товара с названием = {}...", category.getName());

        if (checkName(category.getName(), category.getGlobalProductCategory().getId(), null)) {
            log.error("Категория с названием = {} уже существует в глобальной категории {}.",
                    category.getName(), category.getGlobalProductCategory().getName());
            return false;
        }

        // Авто-добавление обязательных атрибутов
        List<ProductAttribute> mandatory = getMandatoryAttributes();
        for (ProductAttribute ma : mandatory) {
            if (!category.getAttributes().contains(ma)) {
                category.getAttributes().add(ma);
            }
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

    @Transactional
    public boolean editProductCategory(Long id, String inputName, Long globalProductCategoryId, List<Long> attributeIds) {
        return editProductCategory(id, inputName, globalProductCategoryId, attributeIds, null);
    }

    @Transactional
    public boolean editProductCategory(Long id, String inputName, Long globalProductCategoryId,
                                        List<Long> attributeIds, Map<Long, Map<Long, String>> productAttributeValues) {
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

        // Определяем какие атрибуты удаляются
        List<ProductAttribute> oldAttributes = new ArrayList<>(category.getAttributes());
        List<ProductAttribute> newAttributes = (attributeIds != null && !attributeIds.isEmpty())
                ? new ArrayList<>(productAttributeRepository.findAllById(attributeIds))
                : new ArrayList<>();

        // Гарантируем присутствие обязательных атрибутов
        List<ProductAttribute> mandatory = getMandatoryAttributes();
        for (ProductAttribute ma : mandatory) {
            if (!newAttributes.contains(ma)) {
                newAttributes.add(ma);
            }
        }

        // Находим удалённые атрибуты (были в старом списке, но нет в новом)
        List<ProductAttribute> removedAttributes = new ArrayList<>(oldAttributes);
        removedAttributes.removeAll(newAttributes);

        // Удаляем значения атрибутов для товаров этой категории
        for (ProductAttribute removedAttr : removedAttributes) {
            productAttributeValueRepository.deleteByProductCategoryAndAttribute(category, removedAttr);
            log.info("Удалены значения атрибута '{}' для товаров категории '{}'.",
                    removedAttr.getName(), category.getName());
        }

        category.setAttributes(newAttributes);

        try {
            productCategoryRepository.save(category);

            // Создаём значения атрибутов для существующих товаров (при добавлении новых атрибутов)
            if (productAttributeValues != null && !productAttributeValues.isEmpty()) {
                for (Map.Entry<Long, Map<Long, String>> productEntry : productAttributeValues.entrySet()) {
                    Product product = productRepository.findById(productEntry.getKey()).orElse(null);
                    if (product == null) {
                        log.error("Товар с id = {} не найден при создании значений атрибута.", productEntry.getKey());
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                        return false;
                    }
                    for (Map.Entry<Long, String> attrEntry : productEntry.getValue().entrySet()) {
                        ProductAttribute attr = productAttributeRepository.findById(attrEntry.getKey()).orElse(null);
                        if (attr == null) continue;
                        String value = attrEntry.getValue();
                        if (value == null || value.isBlank()) continue;
                        if (!productAttributeValueRepository.existsByProductAndAttribute(product, attr)) {
                            ProductAttributeValue pav = new ProductAttributeValue(product, attr, value.trim());
                            productAttributeValueRepository.save(pav);
                            log.info("Создано значение атрибута '{}' = '{}' для товара '{}'.",
                                    attr.getName(), value.trim(), product.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при сохранении изменений категории товара: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Изменения категории товара успешно сохранены.");
        return true;
    }

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

    @Transactional
    public List<ProductCategoryDTO> getAllProductCategories() {
        List<ProductCategory> categories = productCategoryRepository.findAll();
        return ProductCategoryMapper.INSTANCE.toDTOList(categories);
    }
}
