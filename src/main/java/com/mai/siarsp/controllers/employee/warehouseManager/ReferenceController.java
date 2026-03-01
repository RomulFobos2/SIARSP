package com.mai.siarsp.controllers.employee.warehouseManager;

import com.mai.siarsp.service.employee.manager.ClientService;
import com.mai.siarsp.service.employee.manager.SupplierService;
import com.mai.siarsp.service.employee.manager.VehicleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;

@Controller("warehouseManagerReferenceController")
@RequestMapping("/employee/warehouseManager/reference")
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

    @GetMapping("/allVehicles")
    public String allVehicles(Model model) {
        model.addAttribute("allVehicles", vehicleService.getAllVehicles());
        return "employee/warehouseManager/reference/allVehicles";
    }

    @GetMapping("/allSuppliers")
    public String allSuppliers(Model model) {
        model.addAttribute("allSuppliers", supplierService.getAllSuppliers());
        return "employee/warehouseManager/reference/allSuppliers";
    }

    @GetMapping("/allClients")
    public String allClients(Model model) {
        model.addAttribute("allClients", clientService.getAllClients().stream()
                .sorted(Comparator.comparing(c -> c.getOrganizationName()))
                .toList());
        return "employee/warehouseManager/reference/allClients";
    }
}
