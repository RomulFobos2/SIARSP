package com.mai.siarsp.service.employee;

import com.mai.siarsp.dto.EquipmentTypeDTO;
import com.mai.siarsp.dto.WarehouseEquipmentDTO;
import com.mai.siarsp.enumeration.EquipmentStatus;
import com.mai.siarsp.mapper.EquipmentTypeMapper;
import com.mai.siarsp.mapper.WarehouseEquipmentMapper;
import com.mai.siarsp.models.EquipmentType;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.models.WarehouseEquipment;
import com.mai.siarsp.repo.EquipmentTypeRepository;
import com.mai.siarsp.repo.WarehouseEquipmentRepository;
import com.mai.siarsp.repo.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Сервис управления оборудованием склада и типами оборудования.
 *
 * Операции с оборудованием:
 * - Заведующий складом создаёт новые записи об оборудовании
 * - Руководитель редактирует данные и меняет статус
 *
 * Операции с типами оборудования:
 * - Администратор управляет справочником типов (CRUD)
 */
@Service
@Slf4j
public class WarehouseEquipmentService {

    private final WarehouseEquipmentRepository equipmentRepository;
    private final EquipmentTypeRepository equipmentTypeRepository;
    private final WarehouseRepository warehouseRepository;

    public WarehouseEquipmentService(WarehouseEquipmentRepository equipmentRepository,
                                     EquipmentTypeRepository equipmentTypeRepository,
                                     WarehouseRepository warehouseRepository) {
        this.equipmentRepository = equipmentRepository;
        this.equipmentTypeRepository = equipmentTypeRepository;
        this.warehouseRepository = warehouseRepository;
    }

    // ========== ОБОРУДОВАНИЕ ==========

    /**
     * Возвращает список всего оборудования, отсортированного по наименованию.
     */
    @Transactional(readOnly = true)
    public List<WarehouseEquipmentDTO> getAllEquipment() {
        return WarehouseEquipmentMapper.INSTANCE.toDTOList(
                equipmentRepository.findAllByOrderByNameAsc());
    }

    /**
     * Возвращает данные об оборудовании по ID.
     */
    @Transactional(readOnly = true)
    public Optional<WarehouseEquipmentDTO> getById(Long id) {
        return equipmentRepository.findById(id)
                .map(WarehouseEquipmentMapper.INSTANCE::toDTO);
    }

    /**
     * Возвращает сущность оборудования по ID (для контроллеров).
     */
    @Transactional(readOnly = true)
    public Optional<WarehouseEquipment> getEntityById(Long id) {
        return equipmentRepository.findById(id);
    }

    /**
     * Создаёт новую запись об оборудовании склада.
     * Статус устанавливается в IN_USE по умолчанию.
     *
     * @param name            наименование оборудования
     * @param serialNumber    серийный номер (может быть null)
     * @param productionDate  дата производства (может быть null)
     * @param usefulLifeYears срок полезного использования в годах (может быть null)
     * @param equipmentTypeId ID типа оборудования
     * @param warehouseId     ID склада
     * @return true при успехе, false при ошибке
     */
    @Transactional
    public boolean createEquipment(String name,
                                   String serialNumber,
                                   LocalDate productionDate,
                                   Integer usefulLifeYears,
                                   Long equipmentTypeId,
                                   Long warehouseId) {
        try {
            Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
            if (warehouse == null) {
                log.error("Склад с ID {} не найден", warehouseId);
                return false;
            }
            EquipmentType equipmentType = equipmentTypeRepository.findById(equipmentTypeId).orElse(null);
            if (equipmentType == null) {
                log.error("Тип оборудования с ID {} не найден", equipmentTypeId);
                return false;
            }
            if (equipmentRepository.existsByWarehouseAndName(warehouse, name)) {
                log.error("Оборудование с именем '{}' уже существует на складе '{}'", name, warehouse.getName());
                return false;
            }

            WarehouseEquipment equipment = new WarehouseEquipment(name, equipmentType, warehouse);
            equipment.setSerialNumber(serialNumber);
            equipment.setProductionDate(productionDate);
            equipment.setUsefulLifeYears(usefulLifeYears);
            equipmentRepository.save(equipment);
            log.info("Создано оборудование '{}' на складе '{}'", name, warehouse.getName());
            return true;
        } catch (Exception e) {
            log.error("Ошибка при создании оборудования: {}", e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    /**
     * Обновляет данные оборудования.
     * Доступно руководителю (ROLE_EMPLOYEE_MANAGER).
     *
     * @param id              ID оборудования
     * @param name            новое наименование
     * @param serialNumber    серийный номер
     * @param productionDate  дата производства
     * @param usefulLifeYears срок полезного использования
     * @param equipmentTypeId ID типа оборудования
     * @param warehouseId     ID склада
     * @param status          новый статус
     * @return true при успехе, false при ошибке
     */
    @Transactional
    public boolean updateEquipment(Long id,
                                   String name,
                                   String serialNumber,
                                   LocalDate productionDate,
                                   Integer usefulLifeYears,
                                   Long equipmentTypeId,
                                   Long warehouseId,
                                   EquipmentStatus status) {
        try {
            WarehouseEquipment equipment = equipmentRepository.findById(id).orElse(null);
            if (equipment == null) {
                log.error("Оборудование с ID {} не найдено", id);
                return false;
            }
            Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
            if (warehouse == null) {
                log.error("Склад с ID {} не найден", warehouseId);
                return false;
            }
            EquipmentType equipmentType = equipmentTypeRepository.findById(equipmentTypeId).orElse(null);
            if (equipmentType == null) {
                log.error("Тип оборудования с ID {} не найден", equipmentTypeId);
                return false;
            }
            if (equipmentRepository.existsByWarehouseAndNameAndIdNot(warehouse, name, id)) {
                log.error("Оборудование с именем '{}' уже существует на складе '{}'", name, warehouse.getName());
                return false;
            }

            equipment.setName(name);
            equipment.setSerialNumber(serialNumber);
            equipment.setProductionDate(productionDate);
            equipment.setUsefulLifeYears(usefulLifeYears);
            equipment.setEquipmentType(equipmentType);
            equipment.setWarehouse(warehouse);
            equipment.setStatus(status);
            equipmentRepository.save(equipment);
            log.info("Обновлено оборудование ID {}: '{}'", id, name);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при обновлении оборудования ID {}: {}", id, e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    /**
     * Удаляет оборудование по ID.
     *
     * @param id ID оборудования
     * @return true при успехе, false при ошибке
     */
    @Transactional
    public boolean deleteEquipment(Long id) {
        try {
            if (!equipmentRepository.existsById(id)) {
                log.error("Оборудование с ID {} не найдено", id);
                return false;
            }
            equipmentRepository.deleteById(id);
            log.info("Удалено оборудование ID {}", id);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при удалении оборудования ID {}: {}", id, e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    // ========== ТИПЫ ОБОРУДОВАНИЯ ==========

    /**
     * Возвращает список всех типов оборудования, отсортированных по наименованию.
     */
    @Transactional(readOnly = true)
    public List<EquipmentTypeDTO> getAllTypes() {
        return EquipmentTypeMapper.INSTANCE.toDTOList(
                equipmentTypeRepository.findAllByOrderByNameAsc());
    }

    /**
     * Возвращает список сущностей всех типов оборудования (для форм).
     */
    @Transactional(readOnly = true)
    public List<EquipmentType> getAllTypeEntities() {
        return equipmentTypeRepository.findAllByOrderByNameAsc();
    }

    /**
     * Возвращает тип оборудования по ID.
     */
    @Transactional(readOnly = true)
    public Optional<EquipmentTypeDTO> getTypeById(Long id) {
        return equipmentTypeRepository.findById(id)
                .map(EquipmentTypeMapper.INSTANCE::toDTO);
    }

    /**
     * Создаёт новый тип оборудования.
     *
     * @param name наименование типа
     * @return true при успехе, false при ошибке
     */
    @Transactional
    public boolean createEquipmentType(String name) {
        try {
            if (equipmentTypeRepository.existsByName(name)) {
                log.error("Тип оборудования '{}' уже существует", name);
                return false;
            }
            equipmentTypeRepository.save(new EquipmentType(name));
            log.info("Создан тип оборудования '{}'", name);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при создании типа оборудования: {}", e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    /**
     * Обновляет наименование типа оборудования.
     *
     * @param id   ID типа
     * @param name новое наименование
     * @return true при успехе, false при ошибке
     */
    @Transactional
    public boolean updateEquipmentType(Long id, String name) {
        try {
            EquipmentType type = equipmentTypeRepository.findById(id).orElse(null);
            if (type == null) {
                log.error("Тип оборудования с ID {} не найден", id);
                return false;
            }
            if (equipmentTypeRepository.existsByNameAndIdNot(name, id)) {
                log.error("Тип оборудования '{}' уже существует", name);
                return false;
            }
            type.setName(name);
            equipmentTypeRepository.save(type);
            log.info("Обновлён тип оборудования ID {}: '{}'", id, name);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при обновлении типа оборудования ID {}: {}", id, e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    /**
     * Удаляет тип оборудования.
     * Нельзя удалить, если есть оборудование данного типа.
     *
     * @param id ID типа
     * @return true при успехе, false при ошибке
     */
    @Transactional
    public boolean deleteEquipmentType(Long id) {
        try {
            EquipmentType type = equipmentTypeRepository.findById(id).orElse(null);
            if (type == null) {
                log.error("Тип оборудования с ID {} не найден", id);
                return false;
            }
            equipmentTypeRepository.deleteById(id);
            log.info("Удалён тип оборудования ID {}", id);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при удалении типа оборудования ID {} (возможно, используется): {}", id, e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }
}
