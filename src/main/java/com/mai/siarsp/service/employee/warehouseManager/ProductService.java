package com.mai.siarsp.service.employee.warehouseManager;

import com.mai.siarsp.dto.ProductDTO;
import com.mai.siarsp.enumeration.WarehouseType;
import com.mai.siarsp.mapper.ProductMapper;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.ProductAttribute;
import com.mai.siarsp.models.ProductAttributeValue;
import com.mai.siarsp.models.ProductCategory;
import com.mai.siarsp.repo.*;
import com.mai.siarsp.service.general.ImageService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * Сервис товаров в контуре роли: поддержка каталога, валидация данных и операции, нужные конкретному подразделению.
 */

@Service("warehouseManagerProductService")
@Getter
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final OrderedProductRepository orderedProductRepository;
    private final RequestedProductRepository requestedProductRepository;
    private final ZoneProductRepository zoneProductRepository;
    private final SupplyRepository supplyRepository;
    private final WriteOffActRepository writeOffActRepository;

    public ProductService(ProductRepository productRepository,
                          ProductCategoryRepository productCategoryRepository,
                          ProductAttributeRepository productAttributeRepository,
                          ProductAttributeValueRepository productAttributeValueRepository,
                          OrderedProductRepository orderedProductRepository,
                          RequestedProductRepository requestedProductRepository,
                          ZoneProductRepository zoneProductRepository,
                          SupplyRepository supplyRepository,
                          WriteOffActRepository writeOffActRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productAttributeRepository = productAttributeRepository;
        this.productAttributeValueRepository = productAttributeValueRepository;
        this.orderedProductRepository = orderedProductRepository;
        this.requestedProductRepository = requestedProductRepository;
        this.zoneProductRepository = zoneProductRepository;
        this.supplyRepository = supplyRepository;
        this.writeOffActRepository = writeOffActRepository;
    }

    public boolean checkArticle(String article, Long id) {
        if (article == null || article.isBlank()) {
            return false;
        }

        if (id != null) {
            return productRepository.existsByArticleAndIdNot(article, id);
        }

        return productRepository.existsByArticle(article);
    }

    @Transactional
    public Optional<Long> saveProduct(Product product, Long categoryId, MultipartFile inputFileField,
                                      Map<String, String> attributeValues) {
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

        // Автосинхронизация: stockQuantity не может быть меньше quantityForStock
        if (product.getQuantityForStock() > product.getStockQuantity()) {
            product.setStockQuantity(product.getQuantityForStock());
        }

        try {
            product.setImage(ImageService.uploadImage(inputFileField));
            productRepository.save(product);
            saveAttributeValues(product, attributeValues);
        } catch (Exception e) {
            log.error("Ошибка при сохранении товара: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Optional.empty();
        }

        return Optional.of(product.getId());
    }

    @Transactional
    public Optional<Long> editProduct(Long id,
                                      String inputName,
                                      String inputArticle,
                                      int inputStockQuantity,
                                      int inputQuantityForStock,
                                      WarehouseType inputWarehouseType,
                                      Long inputCategoryId,
                                      MultipartFile inputFileField,
                                      Map<String, String> attributeValues) {
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
        // Автосинхронизация: stockQuantity не может быть меньше quantityForStock
        if (product.getQuantityForStock() > product.getStockQuantity()) {
            product.setStockQuantity(product.getQuantityForStock());
        }
        product.setWarehouseType(inputWarehouseType);
        product.setCategory(categoryOptional.get());

        try {
            if (inputFileField != null && !inputFileField.isEmpty()) {
                product.setImage(ImageService.uploadImage(inputFileField));
            }
            productRepository.save(product);
            updateAttributeValues(product, attributeValues);
        } catch (Exception e) {
            log.error("Ошибка при обновлении товара: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Optional.empty();
        }

        return Optional.of(product.getId());
    }

    @Transactional
    public boolean deleteProduct(Long id) {
        Optional<Product> productOptional = productRepository.findById(id);

        if (productOptional.isEmpty()) {
            log.error("Товар с id = {} не найден.", id);
            return false;
        }

        // Проверка зависимостей
        if (orderedProductRepository.countByProductId(id) > 0) {
            log.error("Невозможно удалить товар id={}: используется в заказах клиентов.", id);
            return false;
        }
        if (requestedProductRepository.countByProductId(id) > 0) {
            log.error("Невозможно удалить товар id={}: используется в заявках на поставку.", id);
            return false;
        }
        if (zoneProductRepository.countByProductId(id) > 0) {
            log.error("Невозможно удалить товар id={}: размещён на складе.", id);
            return false;
        }
        if (supplyRepository.countByProductId(id) > 0) {
            log.error("Невозможно удалить товар id={}: присутствует в поставках.", id);
            return false;
        }
        if (writeOffActRepository.countByProductId(id) > 0) {
            log.error("Невозможно удалить товар id={}: упоминается в актах списания.", id);
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

    @Transactional
    public List<ProductDTO> getAllProducts() {
        return ProductMapper.INSTANCE.toDTOList(productRepository.findAll());
    }

    private void saveAttributeValues(Product product, Map<String, String> attributeValues) {
        if (attributeValues == null || attributeValues.isEmpty()) return;

        for (Map.Entry<String, String> entry : attributeValues.entrySet()) {
            Long attrId = Long.parseLong(entry.getKey());
            String value = entry.getValue();
            if (value == null || value.isBlank()) continue;

            Optional<ProductAttribute> attrOpt = productAttributeRepository.findById(attrId);
            if (attrOpt.isPresent()) {
                ProductAttributeValue pav = new ProductAttributeValue(product, attrOpt.get(), value.trim());
                productAttributeValueRepository.save(pav);
            }
        }
    }

    private void updateAttributeValues(Product product, Map<String, String> attributeValues) {
        // 1. Создаём карту новых значений: attrId → value
        Map<Long, String> newValues = new HashMap<>();
        if (attributeValues != null) {
            for (Map.Entry<String, String> entry : attributeValues.entrySet()) {
                Long attrId = Long.parseLong(entry.getKey());
                String value = entry.getValue();
                if (value != null && !value.isBlank()) {
                    newValues.put(attrId, value.trim());
                }
            }
        }

        // 2. Обновить существующие или удалить неактуальные
        Iterator<ProductAttributeValue> it = product.getAttributeValues().iterator();
        while (it.hasNext()) {
            ProductAttributeValue pav = it.next();
            Long attrId = pav.getAttribute().getId();
            if (newValues.containsKey(attrId)) {
                pav.setValue(newValues.get(attrId));
                newValues.remove(attrId);
            } else {
                it.remove();
            }
        }

        // 3. Добавить новые атрибуты
        for (Map.Entry<Long, String> entry : newValues.entrySet()) {
            Optional<ProductAttribute> attrOpt = productAttributeRepository.findById(entry.getKey());
            if (attrOpt.isPresent()) {
                ProductAttributeValue pav = new ProductAttributeValue(product, attrOpt.get(), entry.getValue());
                product.getAttributeValues().add(pav);
            }
        }

        productRepository.save(product);
    }
}
