package com.mai.siarsp.service.employee.manager;

import com.mai.siarsp.dto.VehicleDTO;
import com.mai.siarsp.enumeration.VehicleStatus;
import com.mai.siarsp.enumeration.VehicleType;
import com.mai.siarsp.mapper.VehicleMapper;
import com.mai.siarsp.models.Vehicle;
import com.mai.siarsp.repo.VehicleRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления транспортными средствами (Vehicle)
 *
 * Предоставляет CRUD-операции для работы с автомобилями предприятия:
 * - Проверка уникальности гос. номера
 * - Создание нового ТС (статус AVAILABLE автоматически)
 * - Редактирование данных ТС (включая смену статуса)
 * - Удаление ТС (с защитой при наличии задач на доставку)
 * - Получение списка всех ТС
 */
@Service
@Getter
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    /**
     * Проверяет, существует ли ТС с указанным гос. номером
     *
     * @param registrationNumber гос. номер для проверки
     * @param id ID текущего ТС (null при создании, не null при редактировании — для исключения из проверки)
     * @return true если такой гос. номер уже занят
     */
    public boolean checkRegistrationNumber(String registrationNumber, Long id) {
        if (registrationNumber == null || registrationNumber.isBlank()) {
            return false;
        }
        if (id != null) {
            return vehicleRepository.existsByRegistrationNumberAndIdNot(registrationNumber, id);
        } else {
            return vehicleRepository.existsByRegistrationNumber(registrationNumber);
        }
    }

    /**
     * Сохраняет новое транспортное средство
     * Статус устанавливается автоматически через конструктор Vehicle (AVAILABLE)
     *
     * @param vehicle объект ТС для сохранения
     * @return true при успешном сохранении, false при ошибке
     */
    @Transactional
    public boolean saveVehicle(Vehicle vehicle) {
        log.info("Начинаем сохранение ТС с гос. номером = {}...", vehicle.getRegistrationNumber());

        if (checkRegistrationNumber(vehicle.getRegistrationNumber(), null)) {
            log.error("ТС с гос. номером = {} уже существует.", vehicle.getRegistrationNumber());
            return false;
        }

        try {
            vehicleRepository.save(vehicle);
        } catch (Exception e) {
            log.error("Ошибка при сохранении ТС {}: {}", vehicle.getRegistrationNumber(), e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("ТС {} успешно сохранено.", vehicle.getRegistrationNumber());
        return true;
    }

    /**
     * Редактирует существующее транспортное средство
     *
     * @param id ID редактируемого ТС
     * @param inputRegistrationNumber новый гос. номер
     * @param inputBrand новая марка
     * @param inputModel новая модель
     * @param inputYear новый год выпуска (может быть null)
     * @param inputVin новый VIN-код (может быть null)
     * @param inputLoadCapacity новая грузоподъёмность
     * @param inputVolumeCapacity новая объёмная вместимость
     * @param inputType новый тип ТС
     * @param inputStatus новый статус ТС
     * @return true при успешном сохранении, false при ошибке
     */
    @Transactional
    public boolean editVehicle(Long id, String inputRegistrationNumber, String inputBrand, String inputModel,
                               Integer inputYear, String inputVin, Double inputLoadCapacity,
                               Double inputVolumeCapacity, VehicleType inputType, VehicleStatus inputStatus) {
        Optional<Vehicle> vehicleOptional = vehicleRepository.findById(id);

        if (vehicleOptional.isEmpty()) {
            log.error("Не найдено ТС с id = {}.", id);
            return false;
        }

        if (checkRegistrationNumber(inputRegistrationNumber, id)) {
            log.error("ТС с гос. номером = {} уже существует.", inputRegistrationNumber);
            return false;
        }

        Vehicle vehicle = vehicleOptional.get();
        log.info("Начинаем редактирование ТС с id = {}...", id);

        vehicle.setRegistrationNumber(inputRegistrationNumber);
        vehicle.setBrand(inputBrand);
        vehicle.setModel(inputModel);
        vehicle.setYear(inputYear);
        vehicle.setVin(inputVin != null ? inputVin : "");
        vehicle.setLoadCapacity(inputLoadCapacity);
        vehicle.setVolumeCapacity(inputVolumeCapacity);
        vehicle.setType(inputType);
        vehicle.setStatus(inputStatus);

        try {
            vehicleRepository.save(vehicle);
        } catch (Exception e) {
            log.error("Ошибка при сохранении изменений ТС: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Изменения ТС успешно сохранены.");
        return true;
    }

    /**
     * Удаляет транспортное средство
     * Удаление невозможно, если у ТС есть связанные задачи на доставку (DeliveryTask)
     *
     * @param id ID удаляемого ТС
     * @return true при успешном удалении, false при ошибке или наличии связей
     */
    @Transactional
    public boolean deleteVehicle(Long id) {
        Optional<Vehicle> vehicleOptional = vehicleRepository.findById(id);

        if (vehicleOptional.isEmpty()) {
            log.error("Не найдено ТС с id = {}.", id);
            return false;
        }

        Vehicle vehicle = vehicleOptional.get();

        if (!vehicle.getDeliveryTasks().isEmpty()) {
            log.error("Невозможно удалить ТС {} — имеются связанные задачи на доставку ({} шт.).",
                    vehicle.getRegistrationNumber(), vehicle.getDeliveryTasks().size());
            return false;
        }

        log.info("Начинаем удаление ТС {}...", vehicle.getRegistrationNumber());

        try {
            vehicleRepository.delete(vehicle);
        } catch (Exception e) {
            log.error("Ошибка при удалении ТС: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("ТС успешно удалено.");
        return true;
    }

    /**
     * Возвращает список всех транспортных средств в виде DTO
     *
     * @return список VehicleDTO
     */
    public List<VehicleDTO> getAllVehicles() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        return VehicleMapper.INSTANCE.toDTOList(vehicles);
    }

    /**
     * Возвращает ТС по ID в виде DTO
     *
     * @param id ID транспортного средства
     * @return VehicleDTO или null если не найдено
     */
    @Transactional(readOnly = true)
    public VehicleDTO getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .map(VehicleMapper.INSTANCE::toDTO)
                .orElse(null);
    }
}
