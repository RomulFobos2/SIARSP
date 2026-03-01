package com.mai.siarsp.controllers.employee.accounter;

import com.mai.siarsp.dto.ClientDTO;
import com.mai.siarsp.dto.SupplierDTO;
import com.mai.siarsp.dto.VehicleDTO;
import com.mai.siarsp.service.employee.manager.ClientService;
import com.mai.siarsp.service.employee.manager.SupplierService;
import com.mai.siarsp.service.employee.manager.VehicleService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;

@Controller("accounterReferenceController")
@RequestMapping("/employee/accounter/reference")
public class ReferenceController {

    private final VehicleService vehicleService;
    private final SupplierService supplierService;
    private final ClientService clientService;

    public ReferenceController(VehicleService vehicleService,
                               SupplierService supplierService,
                               ClientService clientService) {
        this.vehicleService = vehicleService;
        this.supplierService = supplierService;
        this.clientService = clientService;
    }

    @Transactional
    @GetMapping("/allVehicles")
    public String allVehicles(Model model) {
        model.addAttribute("allVehicles", vehicleService.getAllVehicles());
        return "employee/accounter/reference/allVehicles";
    }

    @Transactional
    @GetMapping("/allSuppliers")
    public String allSuppliers(Model model) {
        model.addAttribute("allSuppliers", supplierService.getAllSuppliers());
        return "employee/accounter/reference/allSuppliers";
    }

    @Transactional
    @GetMapping("/allClients")
    public String allClients(Model model) {
        model.addAttribute("allClients", clientService.getAllClients().stream()
                .sorted(Comparator.comparing(c -> c.getOrganizationName()))
                .toList());
        return "employee/accounter/reference/allClients";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsVehicle/{id}")
    public String detailsVehicle(@PathVariable Long id, Model model) {
        VehicleDTO vehicleDTO = vehicleService.getVehicleById(id);
        if (vehicleDTO == null) {
            return "redirect:/employee/accounter/reference/allVehicles";
        }
        model.addAttribute("vehicleDTO", vehicleDTO);
        return "employee/accounter/reference/detailsVehicle";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsSupplier/{id}")
    public String detailsSupplier(@PathVariable Long id, Model model) {
        SupplierDTO supplierDTO = supplierService.getSupplierById(id);
        if (supplierDTO == null) {
            return "redirect:/employee/accounter/reference/allSuppliers";
        }
        model.addAttribute("supplierDTO", supplierDTO);
        return "employee/accounter/reference/detailsSupplier";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsClient/{id}")
    public String detailsClient(@PathVariable Long id, Model model) {
        ClientDTO clientDTO = clientService.getClientById(id);
        if (clientDTO == null) {
            return "redirect:/employee/accounter/reference/allClients";
        }
        model.addAttribute("clientDTO", clientDTO);
        return "employee/accounter/reference/detailsClient";
    }
}
