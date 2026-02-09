package com.mai.siarsp.controllers.employee.manager;

import com.mai.siarsp.dto.SupplierDTO;
import com.mai.siarsp.mapper.SupplierMapper;
import com.mai.siarsp.models.Supplier;
import com.mai.siarsp.service.employee.manager.SupplierService;
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
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping("/employee/manager/suppliers/check-inn")
    public ResponseEntity<Map<String, Boolean>> checkInn(@RequestParam String inn,
                                                          @RequestParam(required = false) Long id) {
        log.info("Проверка ИНН поставщика {}.", inn);
        boolean exists = supplierService.checkInn(inn, id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    @Transactional
    @GetMapping("/employee/manager/suppliers/allSuppliers")
    public String allSuppliers(Model model) {
        model.addAttribute("allSuppliers", supplierService.getAllSuppliers().stream()
                .sorted(Comparator.comparing(SupplierDTO::getName))
                .toList());
        return "employee/manager/suppliers/allSuppliers";
    }

    @GetMapping("/employee/manager/suppliers/addSupplier")
    public String addSupplier() {
        return "employee/manager/suppliers/addSupplier";
    }

    @PostMapping("/employee/manager/suppliers/addSupplier")
    public String addSupplier(@RequestParam String inputName,
                              @RequestParam(required = false) String inputContactInfo,
                              @RequestParam(required = false) String inputAddress,
                              @RequestParam(required = false) String inputInn,
                              @RequestParam(required = false) String inputKpp,
                              @RequestParam(required = false) String inputOgrn,
                              @RequestParam(required = false) String inputPaymentAccount,
                              @RequestParam(required = false) String inputBik,
                              @RequestParam(required = false) String inputBank,
                              @RequestParam String inputDirectorLastName,
                              @RequestParam String inputDirectorFirstName,
                              @RequestParam String inputDirectorPatronymicName,
                              Model model) {
        Supplier supplier = new Supplier(inputName, inputContactInfo, inputAddress,
                inputInn, inputKpp, inputOgrn,
                inputPaymentAccount, inputBik, inputBank,
                inputDirectorLastName, inputDirectorFirstName, inputDirectorPatronymicName);

        if (!supplierService.saveSupplier(supplier)) {
            model.addAttribute("supplierError", "Ошибка при сохранении поставщика.");
            return "employee/manager/suppliers/addSupplier";
        }

        return "redirect:/employee/manager/suppliers/detailsSupplier/" + supplier.getId();
    }

    @Transactional
    @GetMapping("/employee/manager/suppliers/detailsSupplier/{id}")
    public String detailsSupplier(@PathVariable(value = "id") long id, Model model) {
        if (!supplierService.getSupplierRepository().existsById(id)) {
            return "redirect:/employee/manager/suppliers/allSuppliers";
        }
        Supplier supplier = supplierService.getSupplierRepository().findById(id).get();
        SupplierDTO supplierDTO = SupplierMapper.INSTANCE.toDTO(supplier);

        model.addAttribute("supplierDTO", supplierDTO);
        return "employee/manager/suppliers/detailsSupplier";
    }

    @Transactional
    @GetMapping("/employee/manager/suppliers/editSupplier/{id}")
    public String editSupplier(@PathVariable(value = "id") long id, Model model) {
        if (!supplierService.getSupplierRepository().existsById(id)) {
            return "redirect:/employee/manager/suppliers/allSuppliers";
        }
        Supplier supplier = supplierService.getSupplierRepository().findById(id).get();
        SupplierDTO supplierDTO = SupplierMapper.INSTANCE.toDTO(supplier);

        model.addAttribute("supplierDTO", supplierDTO);
        return "employee/manager/suppliers/editSupplier";
    }

    @PostMapping("/employee/manager/suppliers/editSupplier/{id}")
    public String editSupplier(@PathVariable(value = "id") long id,
                               @RequestParam String inputName,
                               @RequestParam(required = false) String inputContactInfo,
                               @RequestParam(required = false) String inputAddress,
                               @RequestParam(required = false) String inputInn,
                               @RequestParam(required = false) String inputKpp,
                               @RequestParam(required = false) String inputOgrn,
                               @RequestParam(required = false) String inputPaymentAccount,
                               @RequestParam(required = false) String inputBik,
                               @RequestParam(required = false) String inputBank,
                               @RequestParam String inputDirectorLastName,
                               @RequestParam String inputDirectorFirstName,
                               @RequestParam String inputDirectorPatronymicName,
                               RedirectAttributes redirectAttributes) {
        if (!supplierService.editSupplier(id, inputName, inputContactInfo, inputAddress,
                inputInn, inputKpp, inputOgrn, inputPaymentAccount, inputBik, inputBank,
                inputDirectorLastName, inputDirectorFirstName, inputDirectorPatronymicName)) {
            redirectAttributes.addFlashAttribute("supplierError", "Ошибка при сохранении изменений.");
            return "redirect:/employee/manager/suppliers/editSupplier/" + id;
        }

        return "redirect:/employee/manager/suppliers/detailsSupplier/" + id;
    }

    @GetMapping("/employee/manager/suppliers/deleteSupplier/{id}")
    public String deleteSupplier(@PathVariable(value = "id") long id, RedirectAttributes redirectAttributes) {
        if (!supplierService.deleteSupplier(id)) {
            redirectAttributes.addFlashAttribute("deleteError",
                    "Невозможно удалить поставщика. Убедитесь, что у него нет связанных поставок.");
            return "redirect:/employee/manager/suppliers/detailsSupplier/" + id;
        }

        return "redirect:/employee/manager/suppliers/allSuppliers";
    }
}
