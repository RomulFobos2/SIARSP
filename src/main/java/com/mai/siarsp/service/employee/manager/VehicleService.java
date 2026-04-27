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
 * Сервис транспорта: учет машин и их статуса готовности к выполнению задач на доставку.
 */

@Service
@Getter
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

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

    @Transactional
    public boolean editVehicle(Long id, String inputRegistrationNumber, String inputBrand, String inputModel,
                               Integer inputYear, String inputVin, Integer inputCurrentMileage,
                               Double inputLoadCapacity, Double inputVolumeCapacity,
                               VehicleType inputType, VehicleStatus inputStatus) {
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
        vehicle.setCurrentMileage(inputCurrentMileage);
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

    public List<VehicleDTO> getAllVehicles() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        return VehicleMapper.INSTANCE.toDTOList(vehicles);
    }

    @Transactional(readOnly = true)
    public VehicleDTO getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .map(VehicleMapper.INSTANCE::toDTO)
                .orElse(null);
    }
}
