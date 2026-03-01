package com.mai.siarsp.controllers.employee.manager;

import com.mai.siarsp.dto.ClientDTO;
import com.mai.siarsp.mapper.ClientMapper;
import com.mai.siarsp.models.Client;
import com.mai.siarsp.service.employee.manager.ClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/employee/manager/clients/check-inn")
    public ResponseEntity<Map<String, Boolean>> checkInn(@RequestParam String inn,
                                                         @RequestParam(required = false) Long id) {
        log.info("Проверка ИНН {}.", inn);
        boolean exists = clientService.checkInn(inn, id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    @Transactional
    @GetMapping("/employee/manager/clients/allClients")
    public String allClients(Model model) {
        model.addAttribute("allClients", clientService.getAllClients().stream()
                .sorted(Comparator.comparing(ClientDTO::getOrganizationName))
                .toList());
        return "employee/manager/clients/allClients";
    }

    @GetMapping("/employee/manager/clients/addClient")
    public String addClient() {
        return "employee/manager/clients/addClient";
    }

    @PostMapping("/employee/manager/clients/addClient")
    public String addClient(@RequestParam String inputOrganizationType,
                            @RequestParam String inputOrganizationName,
                            @RequestParam(required = false) String inputInn,
                            @RequestParam(required = false) String inputKpp,
                            @RequestParam(required = false) String inputOgrn,
                            @RequestParam(required = false) String inputLegalAddress,
                            @RequestParam String inputDeliveryAddress,
                            @RequestParam(required = false) Double inputDeliveryLatitude,
                            @RequestParam(required = false) Double inputDeliveryLongitude,
                            @RequestParam(required = false) String inputContactPerson,
                            @RequestParam(required = false) String inputPhoneNumber,
                            @RequestParam(required = false) String inputEmail,
                            Model model) {
        Client client = new Client(inputOrganizationType, inputOrganizationName,
                inputDeliveryAddress, inputContactPerson);
        client.setInn(inputInn);
        client.setKpp(inputKpp);
        client.setOgrn(inputOgrn);
        client.setLegalAddress(inputLegalAddress);
        client.setDeliveryLatitude(inputDeliveryLatitude);
        client.setDeliveryLongitude(inputDeliveryLongitude);
        client.setPhoneNumber(inputPhoneNumber);
        client.setEmail(inputEmail);

        if (!clientService.saveClient(client)) {
            model.addAttribute("clientError", "Ошибка при сохранении клиента.");
            return "employee/manager/clients/addClient";
        }

        return "redirect:/employee/manager/clients/detailsClient/" + client.getId();
    }

    @Transactional
    @GetMapping("/employee/manager/clients/detailsClient/{id}")
    public String detailsClient(@PathVariable(value = "id") long id, Model model) {
        if (!clientService.getClientRepository().existsById(id)) {
            return "redirect:/employee/manager/clients/allClients";
        }
        Client client = clientService.getClientRepository().findById(id).get();
        ClientDTO clientDTO = ClientMapper.INSTANCE.toDTO(client);

        model.addAttribute("clientDTO", clientDTO);
        return "employee/manager/clients/detailsClient";
    }

    @Transactional
    @GetMapping("/employee/manager/clients/editClient/{id}")
    public String editClient(@PathVariable(value = "id") long id, Model model) {
        if (!clientService.getClientRepository().existsById(id)) {
            return "redirect:/employee/manager/clients/allClients";
        }
        Client client = clientService.getClientRepository().findById(id).get();
        ClientDTO clientDTO = ClientMapper.INSTANCE.toDTO(client);

        model.addAttribute("clientDTO", clientDTO);
        return "employee/manager/clients/editClient";
    }

    @PostMapping("/employee/manager/clients/editClient/{id}")
    public String editClient(@PathVariable(value = "id") long id,
                             @RequestParam String inputOrganizationType,
                             @RequestParam String inputOrganizationName,
                             @RequestParam(required = false) String inputInn,
                             @RequestParam(required = false) String inputKpp,
                             @RequestParam(required = false) String inputOgrn,
                             @RequestParam(required = false) String inputLegalAddress,
                             @RequestParam String inputDeliveryAddress,
                             @RequestParam(required = false) Double inputDeliveryLatitude,
                             @RequestParam(required = false) Double inputDeliveryLongitude,
                             @RequestParam(required = false) String inputContactPerson,
                             @RequestParam(required = false) String inputPhoneNumber,
                             @RequestParam(required = false) String inputEmail,
                             RedirectAttributes redirectAttributes) {
        if (!clientService.editClient(id, inputOrganizationType, inputOrganizationName,
                inputInn, inputKpp, inputOgrn, inputLegalAddress, inputDeliveryAddress,
                inputDeliveryLatitude, inputDeliveryLongitude,
                inputContactPerson, inputPhoneNumber, inputEmail)) {
            redirectAttributes.addFlashAttribute("clientError", "Ошибка при сохранении изменений.");
            return "redirect:/employee/manager/clients/editClient/" + id;
        }

        return "redirect:/employee/manager/clients/detailsClient/" + id;
    }

    @GetMapping("/employee/manager/clients/deleteClient/{id}")
    public String deleteClient(@PathVariable(value = "id") long id, RedirectAttributes redirectAttributes) {
        if (!clientService.deleteClient(id)) {
            redirectAttributes.addFlashAttribute("deleteError",
                    "Невозможно удалить клиента. Убедитесь, что у него нет связанных заказов.");
            return "redirect:/employee/manager/clients/detailsClient/" + id;
        }

        return "redirect:/employee/manager/clients/allClients";
    }
}
