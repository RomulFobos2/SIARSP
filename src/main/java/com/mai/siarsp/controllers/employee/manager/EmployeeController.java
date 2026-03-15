package com.mai.siarsp.controllers.employee.manager;

import com.mai.siarsp.dto.EmployeeDTO;
import com.mai.siarsp.mapper.EmployeeMapper;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.service.employee.EmployeeService;
import com.mai.siarsp.service.general.ContractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Controller("managerEmployeeController")
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/employee/manager/employees/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        log.info("Проверка имени пользователя {}.", username);
        boolean exists = employeeService.checkUserName(username);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/employee/manager/employees/allEmployees")
    public String allEmployees(Model model) {
        model.addAttribute("allEmployees", employeeService.getAllEmployees().stream()
                .sorted(Comparator.comparing(EmployeeDTO::getRoleDescription))
                .toList());
        return "employee/manager/employees/allEmployees";
    }

    @GetMapping("/employee/manager/employees/addEmployee")
    public String addEmployee(Model model) {
        model.addAttribute("allRoles", employeeService.getRoleRepository().findByNameStartingWith("ROLE_EMPLOYEE"));
        return "employee/manager/employees/addEmployee";
    }

    @PostMapping("/employee/manager/employees/addEmployee")
    public String addEmployee(@RequestParam String inputLastName, @RequestParam String inputFirstName,
                              @RequestParam String inputPatronymicName, @RequestParam String inputUsername,
                              @RequestParam String inputPassword,
                              @RequestParam String inputRole,
                              @RequestParam(required = false) String inputSpecialization,
                              @RequestParam(required = false) String inputQualification,
                              @RequestParam(required = false) BigDecimal inputSalary,
                              @RequestParam(required = false) MultipartFile hiringOrderFile,
                              Model model) {
        Employee employee = new Employee(inputLastName, inputFirstName, inputPatronymicName, inputUsername, inputPassword);
        if (!employeeService.saveEmployee(employee, inputRole, inputSpecialization, inputQualification,
                inputSalary, hiringOrderFile)) {
            model.addAttribute("usernameError", "Ошибка при сохранении.");
            model.addAttribute("allRoles", employeeService.getRoleRepository().findByNameStartingWith("ROLE_EMPLOYEE"));
            return "employee/manager/employees/addEmployee";
        } else {
            return "redirect:/employee/manager/employees/detailsEmployee/" + employee.getId();
        }
    }

    @GetMapping("/employee/manager/employees/detailsEmployee/{id}")
    public String detailsEmployee(@PathVariable(value = "id") long id, Model model) {
        if (!employeeService.getEmployeeRepository().existsById(id)) {
            return "redirect:/employee/manager/employees/allEmployees";
        }
        Employee employee = employeeService.getEmployeeRepository().findById(id).get();
        EmployeeDTO employeeDTO = EmployeeMapper.INSTANCE.toDTO(employee);

        model.addAttribute("employeeDTO", employeeDTO);
        return "employee/manager/employees/detailsEmployee";
    }

    @GetMapping("/employee/manager/employees/editEmployee/{id}")
    public String editEmployee(@PathVariable(value = "id") long id, Model model) {
        if (!employeeService.getEmployeeRepository().existsById(id)) {
            return "redirect:/employee/manager/employees/allEmployees";
        }
        Employee employee = employeeService.getEmployeeRepository().findById(id).get();
        EmployeeDTO employeeDTO = EmployeeMapper.INSTANCE.toDTO(employee);
        model.addAttribute("allRoles", employeeService.getRoleRepository().findByNameStartingWith("ROLE_EMPLOYEE"));
        model.addAttribute("employeeDTO", employeeDTO);
        return "employee/manager/employees/editEmployee";
    }

    @PostMapping("/employee/manager/employees/editEmployee/{id}")
    public String editEmployee(@PathVariable(value = "id") long id,
                               @RequestParam String inputLastName, @RequestParam String inputFirstName,
                               @RequestParam String inputPatronymicName, @RequestParam String inputUsername,
                               @RequestParam String inputRole,
                               @RequestParam(required = false) String inputSpecialization,
                               @RequestParam(required = false) String inputQualification,
                               @RequestParam(required = false) BigDecimal inputSalary,
                               Model model, RedirectAttributes redirectAttributes) {
        if (!employeeService.editEmployee(id, inputLastName, inputFirstName, inputPatronymicName, inputUsername,
                inputRole, inputSpecialization, inputQualification, inputSalary)) {
            redirectAttributes.addFlashAttribute("usernameError", "Ошибка при сохранении изменений.");
            return "redirect:/employee/manager/employees/editEmployee/" + id;
        } else {
            return "redirect:/employee/manager/employees/detailsEmployee/" + id;
        }
    }

    @PostMapping("/employee/manager/employees/resetEmployeePassword/{id}")
    public ResponseEntity<?> resetEmployeePassword(@PathVariable long id, @RequestBody Map<String, String> payload) {
        String newPassword = payload.get("newPassword");
        if (employeeService.resetEmployeePassword(id, newPassword)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/employee/manager/employees/deactivateEmployee/{id}")
    public String deactivateEmployee(@PathVariable long id,
                                     @RequestParam(required = false) MultipartFile dismissalOrderFile,
                                     RedirectAttributes redirectAttributes) {
        if (employeeService.accountEmployeeLocked(id, dismissalOrderFile)) {
            redirectAttributes.addFlashAttribute("successMessage", "Аккаунт сотрудника деактивирован.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при деактивации аккаунта.");
        }
        return "redirect:/employee/manager/employees/detailsEmployee/" + id;
    }

    @GetMapping("/employee/manager/employees/activateEmployee/{id}")
    public String activateEmployee(@PathVariable long id, RedirectAttributes redirectAttributes) {
        if (employeeService.accountEmployeeUnlocked(id)) {
            redirectAttributes.addFlashAttribute("successMessage", "Аккаунт сотрудника активирован.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при активации аккаунта.");
        }
        return "redirect:/employee/manager/employees/detailsEmployee/" + id;
    }

    @GetMapping("/employee/manager/employees/deleteEmployee/{id}")
    public String deleteEmployee(@PathVariable(value = "id") long id,
                                 Model model, RedirectAttributes redirectAttributes) {
        if (!employeeService.deleteEmployee(id)) {
            redirectAttributes.addFlashAttribute("deleteError", "Ошибка при удалении.");
            return "redirect:/employee/manager/employees/detailsEmployee/" + id;
        } else {
            return "redirect:/employee/manager/employees/allEmployees";
        }
    }

    // ========== СКАЧИВАНИЕ ПРИКАЗОВ ==========

    @GetMapping("/employee/manager/employees/downloadHiringOrder/{id}")
    public ResponseEntity<Resource> downloadHiringOrder(@PathVariable long id) throws IOException {
        if (!employeeService.getEmployeeRepository().existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        Employee employee = employeeService.getEmployeeRepository().findById(id).get();
        if (employee.getHiringOrderFile() == null || employee.getHiringOrderFile().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = ContractService.getContractData(employee.getHiringOrderFile());
        String originalFilename = employee.getHiringOrderFile().contains("_")
                ? employee.getHiringOrderFile().substring(employee.getHiringOrderFile().indexOf("_") + 1)
                : employee.getHiringOrderFile();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(originalFilename).build());
        headers.set(HttpHeaders.CONTENT_TYPE, "application/pdf");
        return ResponseEntity.ok().headers(headers).body(resource);
    }

    @GetMapping("/employee/manager/employees/downloadDismissalOrder/{id}")
    public ResponseEntity<Resource> downloadDismissalOrder(@PathVariable long id) throws IOException {
        if (!employeeService.getEmployeeRepository().existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        Employee employee = employeeService.getEmployeeRepository().findById(id).get();
        if (employee.getDismissalOrderFile() == null || employee.getDismissalOrderFile().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = ContractService.getContractData(employee.getDismissalOrderFile());
        String originalFilename = employee.getDismissalOrderFile().contains("_")
                ? employee.getDismissalOrderFile().substring(employee.getDismissalOrderFile().indexOf("_") + 1)
                : employee.getDismissalOrderFile();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(originalFilename).build());
        headers.set(HttpHeaders.CONTENT_TYPE, "application/pdf");
        return ResponseEntity.ok().headers(headers).body(resource);
    }
}
