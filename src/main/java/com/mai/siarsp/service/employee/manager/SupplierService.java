package com.mai.siarsp.service.employee.manager;

import com.mai.siarsp.dto.SupplierDTO;
import com.mai.siarsp.mapper.SupplierMapper;
import com.mai.siarsp.models.Supplier;
import com.mai.siarsp.repo.SupplierRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.Optional;

@Service
@Getter
@Slf4j
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public boolean checkInn(String inn, Long id) {
        if (inn == null || inn.isBlank()) {
            return false;
        }
        if (id != null) {
            return supplierRepository.existsByInnAndIdNot(inn, id);
        } else {
            return supplierRepository.existsByInn(inn);
        }
    }

    @Transactional
    public boolean saveSupplier(Supplier supplier) {
        log.info("Начинаем сохранение поставщика с названием = {}...", supplier.getName());

        if (supplier.getInn() != null && !supplier.getInn().isBlank() && checkInn(supplier.getInn(), null)) {
            log.error("Поставщик с ИНН = {} уже существует.", supplier.getInn());
            return false;
        }

        try {
            supplierRepository.save(supplier);
        } catch (Exception e) {
            log.error("Ошибка при сохранении поставщика {}: {}", supplier.getName(), e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Поставщик {} успешно сохранён.", supplier.getName());
        return true;
    }

    @Transactional
    public boolean editSupplier(Long id, String inputName, String inputContactInfo, String inputAddress,
                                String inputInn, String inputKpp, String inputOgrn,
                                String inputPaymentAccount, String inputBik, String inputBank,
                                String inputDirectorLastName, String inputDirectorFirstName,
                                String inputDirectorPatronymicName) {
        Optional<Supplier> supplierOptional = supplierRepository.findById(id);

        if (supplierOptional.isEmpty()) {
            log.error("Не найден поставщик с id = {}.", id);
            return false;
        }

        if (inputInn != null && !inputInn.isBlank() && checkInn(inputInn, id)) {
            log.error("Поставщик с ИНН = {} уже существует.", inputInn);
            return false;
        }

        Supplier supplier = supplierOptional.get();
        log.info("Начинаем редактирование поставщика с id = {}...", id);

        supplier.setName(inputName);
        supplier.setContactInfo(inputContactInfo);
        supplier.setAddress(inputAddress);
        supplier.setInn(inputInn);
        supplier.setKpp(inputKpp);
        supplier.setOgrn(inputOgrn);
        supplier.setPaymentAccount(inputPaymentAccount);
        supplier.setBik(inputBik);
        supplier.setBank(inputBank);
        supplier.setDirectorLastName(inputDirectorLastName);
        supplier.setDirectorFirstName(inputDirectorFirstName);
        supplier.setDirectorPatronymicName(inputDirectorPatronymicName != null ? inputDirectorPatronymicName : "");

        try {
            supplierRepository.save(supplier);
        } catch (Exception e) {
            log.error("Ошибка при сохранении изменений поставщика: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Изменения поставщика успешно сохранены.");
        return true;
    }

    @Transactional
    public boolean deleteSupplier(Long id) {
        Optional<Supplier> supplierOptional = supplierRepository.findById(id);

        if (supplierOptional.isEmpty()) {
            log.error("Не найден поставщик с id = {}.", id);
            return false;
        }

        Supplier supplier = supplierOptional.get();

        if (!supplier.getDeliveries().isEmpty()) {
            log.error("Невозможно удалить поставщика {} — имеются связанные поставки ({} шт.).",
                    supplier.getName(), supplier.getDeliveries().size());
            return false;
        }

        log.info("Начинаем удаление поставщика {}...", supplier.getName());

        try {
            supplierRepository.delete(supplier);
        } catch (Exception e) {
            log.error("Ошибка при удалении поставщика: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Поставщик успешно удалён.");
        return true;
    }

    public List<SupplierDTO> getAllSuppliers() {
        List<Supplier> suppliers = supplierRepository.findAll();
        return SupplierMapper.INSTANCE.toDTOList(suppliers);
    }

    /**
     * Возвращает поставщика по ID в виде DTO
     *
     * @param id ID поставщика
     * @return SupplierDTO или null если не найден
     */
    @Transactional(readOnly = true)
    public SupplierDTO getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .map(SupplierMapper.INSTANCE::toDTO)
                .orElse(null);
    }
}
