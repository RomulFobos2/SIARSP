package com.mai.siarsp.service.employee.warehouseManager;

import com.mai.siarsp.dto.ProductAttributeDTO;
import com.mai.siarsp.enumeration.AttributeType;
import com.mai.siarsp.mapper.ProductAttributeMapper;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.ProductAttribute;
import com.mai.siarsp.models.ProductAttributeValue;
import com.mai.siarsp.models.ProductCategory;
import com.mai.siarsp.repo.ProductAttributeRepository;
import com.mai.siarsp.repo.ProductAttributeValueRepository;
import com.mai.siarsp.repo.ProductCategoryRepository;
import com.mai.siarsp.repo.ProductRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.*;

/**
 * Сервис для управления атрибутами (характеристиками) товаров
 *
 * Предоставляет CRUD-операции для работы с атрибутами товаров (EAV-паттерн):
 * - Проверка уникальности названия атрибута
 * - Создание нового атрибута с привязкой к категориям товаров
 * - Редактирование атрибута с пересинхронизацией связей ManyToMany
 * - Удаление атрибута (с защитой при наличии значений ProductAttributeValue)
 * - Получение списка всех атрибутов
 *
 * Важно: ProductAttribute — inverse side связи ManyToMany с ProductCategory.
 * Owning side — ProductCategory (имеет @JoinTable). Поэтому для обновления связи
 * нужно менять category.getAttributes(), а не attribute.getCategories().
 */
@Service
@Getter
@Slf4j
public class ProductAttributeService {

    private final ProductAttributeRepository productAttributeRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductRepository productRepository;

    public ProductAttributeService(ProductAttributeRepository productAttributeRepository,
                                    ProductAttributeValueRepository productAttributeValueRepository,
                                    ProductCategoryRepository productCategoryRepository,
                                    ProductRepository productRepository) {
        this.productAttributeRepository = productAttributeRepository;
        this.productAttributeValueRepository = productAttributeValueRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productRepository = productRepository;
    }

    /**
     * Проверяет, существует ли атрибут с указанным названием
     *
     * @param name название для проверки
     * @param id ID текущего атрибута (null при создании, не null при редактировании)
     * @return true если такое название уже занято
     */
    public boolean checkName(String name, Long id) {
        if (name == null || name.isBlank()) {
            return false;
        }
        if (id != null) {
            return productAttributeRepository.existsByNameAndIdNot(name, id);
        } else {
            return productAttributeRepository.existsByName(name);
        }
    }

    /**
     * Сохраняет новый атрибут товара и привязывает к выбранным категориям
     *
     * Поскольку ProductAttribute — inverse side ManyToMany, привязка к категориям
     * выполняется через owning side: category.getAttributes().add(attribute)
     *
     * @param attribute объект атрибута для сохранения
     * @param categoryIds список ID категорий для привязки (может быть null)
     * @return true при успешном сохранении
     */
    @Transactional
    public boolean saveProductAttribute(ProductAttribute attribute, List<Long> categoryIds) {
        log.info("Начинаем сохранение атрибута с названием = {}...", attribute.getName());

        if (checkName(attribute.getName(), null)) {
            log.error("Атрибут с названием = {} уже существует.", attribute.getName());
            return false;
        }

        try {
            // Сначала сохраняем атрибут (чтобы у него был id)
            productAttributeRepository.save(attribute);

            // Привязываем к категориям через owning side (ProductCategory)
            if (categoryIds != null && !categoryIds.isEmpty()) {
                List<ProductCategory> categories = productCategoryRepository.findAllById(categoryIds);
                for (ProductCategory category : categories) {
                    if (!category.getAttributes().contains(attribute)) {
                        category.getAttributes().add(attribute);
                        productCategoryRepository.save(category);
                        log.info("Атрибут '{}' добавлен в категорию '{}'.", attribute.getName(), category.getName());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при сохранении атрибута {}: {}", attribute.getName(), e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Атрибут {} успешно сохранён.", attribute.getName());
        return true;
    }

    /**
     * Сохраняет новый атрибут товара, привязывает к категориям и создаёт значения
     * для всех существующих товаров в этих категориях.
     *
     * Используется когда в выбранных категориях уже есть товары и пользователь
     * заполнил значения нового атрибута для каждого из них.
     *
     * @param attribute     объект атрибута для сохранения
     * @param categoryIds   список ID категорий для привязки
     * @param productValues карта: productId → значение атрибута
     * @return true при успешном сохранении
     */
    @Transactional
    public boolean saveProductAttributeWithValues(ProductAttribute attribute,
                                                   List<Long> categoryIds,
                                                   Map<Long, String> productValues) {
        if (!saveProductAttribute(attribute, categoryIds)) {
            return false;
        }

        if (productValues != null && !productValues.isEmpty()) {
            try {
                for (Map.Entry<Long, String> entry : productValues.entrySet()) {
                    Product product = productRepository.findById(entry.getKey()).orElse(null);
                    if (product == null) {
                        log.error("Товар с id = {} не найден при создании значений атрибута.", entry.getKey());
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                        return false;
                    }
                    String value = entry.getValue();
                    if (value == null || value.isBlank()) {
                        log.error("Значение атрибута для товара id={} пустое.", entry.getKey());
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                        return false;
                    }
                    ProductAttributeValue pav = new ProductAttributeValue(product, attribute, value.trim());
                    productAttributeValueRepository.save(pav);
                    log.info("Создано значение атрибута '{}' = '{}' для товара '{}'.",
                            attribute.getName(), value.trim(), product.getName());
                }
            } catch (Exception e) {
                log.error("Ошибка при сохранении значений атрибутов: {}", e.getMessage(), e);
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return false;
            }
        }

        return true;
    }

    /**
     * Редактирует существующий атрибут товара и пересинхронизирует связи с категориями
     *
     * @param id ID редактируемого атрибута
     * @param inputName новое название
     * @param inputUnit новая единица измерения (может быть null)
     * @param inputDataType новый тип данных
     * @param categoryIds список ID новых категорий (может быть null)
     * @return true при успешном сохранении
     */
    @Transactional
    public boolean editProductAttribute(Long id, String inputName, String inputUnit,
                                         AttributeType inputDataType, List<Long> categoryIds) {
        Optional<ProductAttribute> attributeOptional = productAttributeRepository.findById(id);

        if (attributeOptional.isEmpty()) {
            log.error("Не найден атрибут с id = {}.", id);
            return false;
        }

        if (checkName(inputName, id)) {
            log.error("Атрибут с названием = {} уже существует.", inputName);
            return false;
        }

        ProductAttribute attribute = attributeOptional.get();
        log.info("Начинаем редактирование атрибута с id = {}...", id);

        // Обновляем поля атрибута
        attribute.setName(inputName);
        attribute.setUnit(inputUnit != null ? inputUnit : "");
        attribute.setDataType(inputDataType);

        try {
            productAttributeRepository.save(attribute);
            log.info("Атрибут '{}' (id={}) сохранён с обновлёнными полями.", attribute.getName(), id);

            // Пересинхронизация ManyToMany через owning side (ProductCategory)
            List<ProductCategory> currentCategories = new ArrayList<>(attribute.getCategories());
            List<ProductCategory> newCategories = (categoryIds != null && !categoryIds.isEmpty())
                    ? productCategoryRepository.findAllById(categoryIds)
                    : new ArrayList<>();

            // Определяем удалённые категории (были в старом списке, но нет в новом)
            List<ProductCategory> removedCategories = new ArrayList<>(currentCategories);
            removedCategories.removeAll(newCategories);

            log.info("Текущие категории: {}, Новые категории: {}, Удалённые категории: {}",
                    currentCategories.stream().map(ProductCategory::getName).toList(),
                    newCategories.stream().map(ProductCategory::getName).toList(),
                    removedCategories.stream().map(ProductCategory::getName).toList());

            // Каскадное удаление ProductAttributeValue для товаров удалённых категорий
            for (ProductCategory removedCat : removedCategories) {
                productAttributeValueRepository.deleteByProductCategoryAndAttribute(removedCat, attribute);
                log.info("Удалены значения атрибута '{}' для товаров категории '{}'.",
                        attribute.getName(), removedCat.getName());
            }

            // 1. Убираем атрибут из ВСЕХ текущих категорий
            for (ProductCategory oldCategory : currentCategories) {
                oldCategory.getAttributes().remove(attribute);
                productCategoryRepository.save(oldCategory);
                log.info("Атрибут '{}' убран из категории '{}'.", attribute.getName(), oldCategory.getName());
            }

            // 2. Добавляем атрибут в новые категории
            for (ProductCategory newCategory : newCategories) {
                newCategory.getAttributes().add(attribute);
                productCategoryRepository.save(newCategory);
                log.info("Атрибут '{}' добавлен в категорию '{}'.", attribute.getName(), newCategory.getName());
            }
        } catch (Exception e) {
            log.error("Ошибка при сохранении изменений атрибута: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Изменения атрибута '{}' (id={}) успешно сохранены.", attribute.getName(), id);
        return true;
    }

    /**
     * Удаляет атрибут товара
     * Удаление невозможно, если для атрибута существуют значения (ProductAttributeValue)
     * Перед удалением атрибут убирается из всех связанных категорий
     *
     * @param id ID удаляемого атрибута
     * @return true при успешном удалении
     */
    @Transactional
    public boolean deleteProductAttribute(Long id) {
        Optional<ProductAttribute> attributeOptional = productAttributeRepository.findById(id);

        if (attributeOptional.isEmpty()) {
            log.error("Не найден атрибут с id = {}.", id);
            return false;
        }

        ProductAttribute attribute = attributeOptional.get();

        // Защита: нельзя удалить если есть значения для товаров
        if (productAttributeValueRepository.existsByAttribute(attribute)) {
            log.error("Невозможно удалить атрибут '{}' — имеются связанные значения товаров.",
                    attribute.getName());
            return false;
        }

        log.info("Начинаем удаление атрибута '{}'...", attribute.getName());

        try {
            // Убираем атрибут из всех категорий (owning side)
            List<ProductCategory> currentCategories = new ArrayList<>(attribute.getCategories());
            for (ProductCategory category : currentCategories) {
                category.getAttributes().remove(attribute);
                productCategoryRepository.save(category);
            }

            productAttributeRepository.delete(attribute);
        } catch (Exception e) {
            log.error("Ошибка при удалении атрибута: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Атрибут успешно удалён.");
        return true;
    }

    /**
     * Возвращает список всех атрибутов в виде DTO
     * @Transactional необходим для доступа к lazy-коллекции categories при маппинге
     *
     * @return список ProductAttributeDTO
     */
    @Transactional
    public List<ProductAttributeDTO> getAllProductAttributes() {
        List<ProductAttribute> attributes = productAttributeRepository.findAll();
        return ProductAttributeMapper.INSTANCE.toDTOList(attributes);
    }
}
