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
 * Сервис для управления глобальными категориями товаров.
 *
 * Обеспечивает CRUD-операции для верхнего уровня классификации товаров
 * (например: "Молочная продукция", "Мясная продукция", "Бакалея").
 *
 * Защита удаления: категория не может быть удалена,
 * если к ней привязаны подкатегории (ProductCategory).
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

    /**
     * Проверка уникальности названия глобальной категории.
     *
     * @param name название для проверки
     * @param id   ID текущей категории (для исключения при редактировании), может быть null
     * @return true если категория с таким названием уже существует
     */
    public boolean checkName(String name, Long id) {
        if (name == null || name.isBlank()) {
            return false;
        }
        if (id != null) {
            return globalProductCategoryRepository.existsByNameAndIdNot(name, id);
        } else {
            return globalProductCategoryRepository.existsByName(name);
        }
    }

    /**
     * Сохранение новой глобальной категории.
     *
     * @param category сущность глобальной категории для сохранения
     * @return true при успешном сохранении, false при ошибке или дублировании имени
     */
    @Transactional
    public boolean saveGlobalProductCategory(GlobalProductCategory category) {
        log.info("Начинаем сохранение глобальной категории с названием = {}...", category.getName());

        if (checkName(category.getName(), null)) {
            log.error("Глобальная категория с названием = {} уже существует.", category.getName());
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

    /**
     * Редактирование существующей глобальной категории.
     *
     * @param id        ID редактируемой категории
     * @param inputName новое название
     * @return true при успешном сохранении изменений
     */
    @Transactional
    public boolean editGlobalProductCategory(Long id, String inputName) {
        Optional<GlobalProductCategory> categoryOptional = globalProductCategoryRepository.findById(id);

        if (categoryOptional.isEmpty()) {
            log.error("Не найдена глобальная категория с id = {}.", id);
            return false;
        }

        if (checkName(inputName, id)) {
            log.error("Глобальная категория с названием = {} уже существует.", inputName);
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

    /**
     * Удаление глобальной категории.
     * Запрещено при наличии связанных подкатегорий (ProductCategory).
     *
     * @param id ID удаляемой категории
     * @return true при успешном удалении
     */
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

    /**
     * Получение списка всех глобальных категорий в формате DTO.
     *
     * @return список GlobalProductCategoryDTO
     */
    public List<GlobalProductCategoryDTO> getAllGlobalProductCategories() {
        List<GlobalProductCategory> categories = globalProductCategoryRepository.findAll();
        return GlobalProductCategoryMapper.INSTANCE.toDTOList(categories);
    }
}
