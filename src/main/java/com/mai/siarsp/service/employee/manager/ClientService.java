package com.mai.siarsp.service.employee.manager;

import com.mai.siarsp.dto.ClientDTO;
import com.mai.siarsp.mapper.ClientMapper;
import com.mai.siarsp.models.Client;
import com.mai.siarsp.repo.ClientRepository;
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
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public boolean checkInn(String inn, Long id) {
        if (inn == null || inn.isBlank()) {
            return false;
        }
        if (id != null) {
            return clientRepository.existsByInnAndIdNot(inn, id);
        } else {
            return clientRepository.existsByInn(inn);
        }
    }

    @Transactional
    public boolean saveClient(Client client) {
        log.info("Начинаем сохранение клиента с названием = {}...", client.getOrganizationName());

        if (client.getInn() != null && !client.getInn().isBlank() && checkInn(client.getInn(), null)) {
            log.error("Клиент с ИНН = {} уже существует.", client.getInn());
            return false;
        }

        try {
            clientRepository.save(client);
        } catch (Exception e) {
            log.error("Ошибка при сохранении клиента {}: {}", client.getOrganizationName(), e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Клиент {} успешно сохранён.", client.getOrganizationName());
        return true;
    }

    @Transactional
    public boolean editClient(Long id, String inputOrganizationType, String inputOrganizationName,
                              String inputInn, String inputKpp, String inputOgrn,
                              String inputLegalAddress, String inputDeliveryAddress,
                              Double inputDeliveryLatitude, Double inputDeliveryLongitude,
                              String inputContactPerson, String inputPhoneNumber, String inputEmail) {
        Optional<Client> clientOptional = clientRepository.findById(id);

        if (clientOptional.isEmpty()) {
            log.error("Не найден клиент с id = {}.", id);
            return false;
        }

        if (inputInn != null && !inputInn.isBlank() && checkInn(inputInn, id)) {
            log.error("Клиент с ИНН = {} уже существует.", inputInn);
            return false;
        }

        Client client = clientOptional.get();
        log.info("Начинаем редактирование клиента с id = {}...", id);

        client.setOrganizationType(inputOrganizationType);
        client.setOrganizationName(inputOrganizationName);
        client.setInn(inputInn);
        client.setKpp(inputKpp);
        client.setOgrn(inputOgrn);
        client.setLegalAddress(inputLegalAddress);
        client.setDeliveryAddress(inputDeliveryAddress);
        client.setDeliveryLatitude(inputDeliveryLatitude);
        client.setDeliveryLongitude(inputDeliveryLongitude);
        client.setContactPerson(inputContactPerson);
        client.setPhoneNumber(inputPhoneNumber);
        client.setEmail(inputEmail);

        try {
            clientRepository.save(client);
        } catch (Exception e) {
            log.error("Ошибка при сохранении изменений клиента: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Изменения клиента успешно сохранены.");
        return true;
    }

    @Transactional
    public boolean deleteClient(Long id) {
        Optional<Client> clientOptional = clientRepository.findById(id);

        if (clientOptional.isEmpty()) {
            log.error("Не найден клиент с id = {}.", id);
            return false;
        }

        Client client = clientOptional.get();

        if (!client.getOrders().isEmpty()) {
            log.error("Невозможно удалить клиента {} — имеются связанные заказы ({} шт.).",
                    client.getOrganizationName(), client.getOrders().size());
            return false;
        }

        log.info("Начинаем удаление клиента {}...", client.getOrganizationName());

        try {
            clientRepository.delete(client);
        } catch (Exception e) {
            log.error("Ошибка при удалении клиента: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Клиент успешно удалён.");
        return true;
    }

    public List<ClientDTO> getAllClients() {
        List<Client> clients = clientRepository.findAll();
        return ClientMapper.INSTANCE.toDTOList(clients);
    }
}
