package com.mai.siarsp.controllers.employee.admin;

import com.mai.siarsp.dto.EmployeeDTO;
import com.mai.siarsp.mapper.EmployeeMapper;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.service.employee.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/employee/admin/employees/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        log.info("Проверка имени пользователя {}.", username);
        boolean exists = employeeService.checkUserName(username);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/employee/admin/employees/allEmployees")
    public String allEmployees(Model model) {
        Optional<? extends UserDetails> currentUser = employeeService.getEmployeeRepository().findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        model.addAttribute("allEmployees", employeeService.getAllEmployees().stream()
                .filter(employeeDTO -> !employeeDTO.getUsername().equals(currentUser.get().getUsername()))
                .sorted(Comparator.comparing(EmployeeDTO::getRoleDescription))
                .toList());
        return "/employee/admin/employees/allEmployees";
    }

    @GetMapping("/employee/admin/employees/addEmployee")
    public String addEmployee(Model model) {
        model.addAttribute("allRoles", employeeService.getRoleRepository().findByNameStartingWith("ROLE_EMPLOYEE"));
        return "/employee/admin/employees/addEmployee";
    }

    @PostMapping("/employee/admin/employees/addEmployee")
    public String addEmployee(@RequestParam String inputLastName, @RequestParam String inputFirstName,
                              @RequestParam String inputPatronymicName, @RequestParam String inputUsername,
                              @RequestParam String inputPassword,
                              @RequestParam String inputRole,
                              Model model) {
        Employee employee = new Employee(inputLastName, inputFirstName, inputPatronymicName, inputUsername, inputPassword);
        if (!employeeService.saveEmployee(employee, inputRole)) {
            model.addAttribute("usernameError", "Ошибка при сохранении.");
            return "employee/admin/employees/addEmployee";
        } else {
            return "redirect:/employee/admin/employees/detailsEmployee/" + employee.getId();
        }
    }

    @GetMapping("/employee/admin/employees/detailsEmployee/{id}")
    public String detailsEmployee(@PathVariable(value = "id") long id, Model model) {
        if (!employeeService.getEmployeeRepository().existsById(id)) {
            return "redirect:/employee/admin/employees/allEmployees";
        }
        Employee employee = employeeService.getEmployeeRepository().findById(id).get();
        EmployeeDTO employeeDTO = EmployeeMapper.INSTANCE.toDTO(employee);

        model.addAttribute("employeeDTO", employeeDTO);
        return "employee/admin/employees/detailsEmployee";
    }

    @GetMapping("/employee/admin/employees/editEmployee/{id}")
    public String editEmployee(@PathVariable(value = "id") long id, Model model) {
        if (!employeeService.getEmployeeRepository().existsById(id)) {
            return "redirect:/employee/admin/employees/allEmployees";
        }
        Employee employee = employeeService.getEmployeeRepository().findById(id).get();
        EmployeeDTO employeeDTO = EmployeeMapper.INSTANCE.toDTO(employee);
        model.addAttribute("allRoles", employeeService.getRoleRepository().findByNameStartingWith("ROLE_EMPLOYEE"));
        model.addAttribute("employeeDTO", employeeDTO);
        return "employee/admin/employees/editEmployee";
    }

    @PostMapping("/employee/admin/employees/editEmployee/{id}")
    public String editEmployee(@PathVariable(value = "id") long id,
                               @RequestParam String inputLastName, @RequestParam String inputFirstName,
                               @RequestParam String inputPatronymicName, @RequestParam String inputUsername,
                               @RequestParam String inputRole,
                               Model model, RedirectAttributes redirectAttributes) {
        if (!employeeService.editEmployee(id, inputFirstName, inputLastName, inputPatronymicName, inputUsername, inputRole)) {
            redirectAttributes.addFlashAttribute("usernameError", "Ошибка при сохранении изменений.");
            return "redirect:/employee/admin/employees/editEmployee/" + id;
        } else {
            return "redirect:/employee/admin/employees/detailsEmployee/" + id;
        }
    }

    @PostMapping("/employee/admin/employees/resetEmployeePassword/{id}")
    public ResponseEntity<?> resetEmployeePassword(@PathVariable long id, @RequestBody Map<String, String> payload) {
        String newPassword = payload.get("newPassword");
        if (employeeService.resetEmployeePassword(id, newPassword)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/employee/admin/employees/resetPassword/{id}")
    public ResponseEntity<?> resetPassword(@PathVariable long id, @RequestBody Map<String, String> payload) {
        String newPassword = payload.get("newPassword");

        if (employeeService.resetEmployeePassword(id, newPassword)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/employee/admin/employees/deleteEmployee/{id}")
    public String deleteEmployee(@PathVariable(value = "id") long id,
                                 Model model, RedirectAttributes redirectAttributes) {
        if (!employeeService.deleteEmployee(id)) {
            redirectAttributes.addFlashAttribute("deleteError", "Ошибка при удалении.");
            return "redirect:/employee/admin/employees/detailsEmployee/" + id;
        } else {
            return "redirect:/employee/admin/employees/allEmployees";
        }
    }

}
