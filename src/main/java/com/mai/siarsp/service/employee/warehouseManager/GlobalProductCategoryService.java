package com.mai.siarsp.service.employee.warehouseManager;

import com.mai.siarsp.dto.GlobalProductCategoryDTO;
import com.mai.siarsp.mapper.GlobalProductCategoryMapper;
import com.mai.siarsp.models.GlobalProductCategory;
import com.mai.siarsp.repo.GlobalProductCategoryRepository;
import com.mai.siarsp.repo.ProductCategoryRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.Optional;

/**
 * Сервис верхнеуровневых категорий: поддерживает единую таксономию для товаров и отчетности.
 */

@Service
@Getter
@Slf4j
public class GlobalProductCategoryService {

    private final GlobalProductCategoryRepository globalProductCategoryRepository;
    private final ProductCategoryRepository productCategoryRepository;

    public GlobalProductCategoryService(GlobalProductCategoryRepository globalProductCategoryRepository,
                                         ProductCategoryRepository productCategoryRepository) {
        this.globalProductCategoryRepository = globalProductCategoryRepository;
        this.productCategoryRepository = productCategoryRepository;
    }

    public boolean checkName(String name, Long id) {
        if (name == null || name.isBlank()) {
            return false;
        }
        // Проверка внутри глобальных категорий (учитываем id при редактировании)
        boolean takenInGlobal = (id != null)
                ? globalProductCategoryRepository.existsByNameAndIdNot(name, id)
                : globalProductCategoryRepository.existsByName(name);
        if (takenInGlobal) {
            return true;
        }
        // Кросс-проверка: имя не должно совпадать ни с одной категорией товаров
        return productCategoryRepository.existsByName(name);
    }

    @Transactional
    public boolean saveGlobalProductCategory(GlobalProductCategory category) {
        log.info("Начинаем сохранение глобальной категории с названием = {}...", category.getName());

        if (checkName(category.getName(), null)) {
            log.error("Имя '{}' уже занято — оно используется глобальной или товарной категорией.", category.getName());
            return false;
        }

        try {
            globalProductCategoryRepository.save(category);
        } catch (Exception e) {
            log.error("Ошибка при сохранении глобальной категории {}: {}", category.getName(), e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Глобальная категория {} успешно сохранена.", category.getName());
        return true;
    }

    @Transactional
    public boolean editGlobalProductCategory(Long id, String inputName) {
        Optional<GlobalProductCategory> categoryOptional = globalProductCategoryRepository.findById(id);

        if (categoryOptional.isEmpty()) {
            log.error("Не найдена глобальная категория с id = {}.", id);
            return false;
        }

        if (checkName(inputName, id)) {
            log.error("Имя '{}' уже занято — оно используется глобальной или товарной категорией.", inputName);
            return false;
        }

        GlobalProductCategory category = categoryOptional.get();
        log.info("Начинаем редактирование глобальной категории с id = {}...", id);

        category.setName(inputName);

        try {
            globalProductCategoryRepository.save(category);
        } catch (Exception e) {
            log.error("Ошибка при сохранении изменений глобальной категории: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Изменения глобальной категории успешно сохранены.");
        return true;
    }

    @Transactional
    public boolean deleteGlobalProductCategory(Long id) {
        Optional<GlobalProductCategory> categoryOptional = globalProductCategoryRepository.findById(id);

        if (categoryOptional.isEmpty()) {
            log.error("Не найдена глобальная категория с id = {}.", id);
            return false;
        }

        GlobalProductCategory category = categoryOptional.get();

        if (productCategoryRepository.existsByGlobalProductCategory(category)) {
            log.error("Невозможно удалить глобальную категорию {} — имеются связанные подкатегории.",
                    category.getName());
            return false;
        }

        log.info("Начинаем удаление глобальной категории {}...", category.getName());

        try {
            globalProductCategoryRepository.delete(category);
        } catch (Exception e) {
            log.error("Ошибка при удалении глобальной категории: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Глобальная категория успешно удалена.");
        return true;
    }

    public List<GlobalProductCategoryDTO> getAllGlobalProductCategories() {
        List<GlobalProductCategory> categories = globalProductCategoryRepository.findAll();
        return GlobalProductCategoryMapper.INSTANCE.toDTOList(categories);
    }
}
