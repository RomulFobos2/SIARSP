package com.mai.siarsp.controllers.general;

import com.mai.siarsp.dto.EmployeeDTO;
import com.mai.siarsp.dto.VisitorDTO;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.Visitor;
import com.mai.siarsp.service.employee.EmployeeService;
import com.mai.siarsp.service.visitor.VisitorService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    private final EmployeeService employeeService;
    private final VisitorService visitorService;

    public MainController(EmployeeService employeeService, VisitorService visitorService) {
        this.employeeService = employeeService;
        this.visitorService = visitorService;
    }

    @GetMapping("/")
    public String home(Model model) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Employee) {
            EmployeeDTO currentEmployeeDTO = employeeService.getAuthenticationEmployeeDTO();
            if(currentEmployeeDTO != null && currentEmployeeDTO.isNeedChangePass()){
                return "redirect:/employee/change-password";
            }
        } else if (principal instanceof Visitor) {
            VisitorDTO currentVisitorDTO = visitorService.getAuthenticationVisitorDTO();
            if(currentVisitorDTO != null && currentVisitorDTO.isNeedChangePass()){
                return "redirect:/visitor/change-password";
            }
        }
        return "general/home";
    }
}
