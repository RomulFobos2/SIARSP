package com.mai.siarsp.controllers.visitor;

import com.mai.siarsp.component.RandomPasswordGenerator;
import com.mai.siarsp.dto.VisitorDTO;
import com.mai.siarsp.models.Visitor;
import com.mai.siarsp.service.general.MailService;
import com.mai.siarsp.service.visitor.VisitorService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Optional;

@Controller
public class MainVisitorController {

    private static final Logger log = LoggerFactory.getLogger(MainVisitorController.class);
    private final VisitorService visitorService;
    private final MailService mailService;

    public MainVisitorController(VisitorService visitorService, MailService mailService) {
        this.visitorService = visitorService;
        this.mailService = mailService;
    }

    @GetMapping("/visitor/login")
    public String visitorLogin(Model model) {
        return "visitor/general/login";
    }

    @GetMapping("/visitor/change-password")
    public String visitorChangePassword(Model model) {
        VisitorDTO currentVisitorDTO = visitorService.getAuthenticationVisitorDTO();
        model.addAttribute("currentVisitor", currentVisitorDTO);
        return "visitor/general/change-password";
    }

    @PostMapping("/visitor/change-password")
    public String changePassword(@RequestParam String passwordNew, @RequestParam String passwordConfirm, Model model) {
        if (passwordNew.equals(passwordConfirm)) {
            if(visitorService.changePassword(passwordNew)){
                return "redirect:/logout";
            }
            else {
                model.addAttribute("passwordError", "Ошибка при сохранении. Повторите попытку.");
                return "visitor/general/change-password";
            }
        }
        else {
            model.addAttribute("passwordError", "Пароли не совпадают. Подтвердите новый пароль верно.");
            return "visitor/general/change-password";
        }
    }

    @GetMapping("/visitor/profile")
    public String profile(Model model) {
        VisitorDTO currentVisitorDTO = visitorService.getAuthenticationVisitorDTO();
        if (currentVisitorDTO != null){
            model.addAttribute("currentVisitor", currentVisitorDTO);
            String strRoleName = visitorService.getRoleRepository().findByName(currentVisitorDTO.getRoleName()).orElseThrow().getDescription();
            model.addAttribute("strRoleName", strRoleName);
        }
        return "visitor/general/profile";
    }

    @GetMapping("/visitor/editProfile")
    public String editVisitor(Model model, HttpServletRequest request) {
        Optional<Visitor> currentVisitor = visitorService.getVisitorRepository().findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName());

        if (currentVisitor.isEmpty()) {
            return "redirect:/visitor/profile";
        }

        model.addAttribute("currentVisitor", currentVisitor.get());
        request.getSession().setAttribute("visitor", currentVisitor.get());
        model.addAttribute("maxDate", LocalDate.now().minusYears(14).toString());
        return "visitor/general/editProfile";
    }


    @PostMapping("/visitor/editProfile")
    public String editVisitor(@RequestParam String inputLastName,
                              @RequestParam String inputFirstName,
                              @RequestParam String inputPatronymicName,
                              @RequestParam String inputSex,
                              @RequestParam LocalDate birthday,
                              @RequestParam String username,
                              @RequestParam String inputMobileNumber,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        Visitor originalVisitor = (Visitor) request.getSession().getAttribute("visitor");

        boolean isUsernameChanged = !originalVisitor.getUsername().equals(username);

        if (isUsernameChanged) {
            String oneTimePassword = RandomPasswordGenerator.getOneTimePassword();
            if (!mailService.sendOneTimePasswordMail(username, oneTimePassword)) {
                redirectAttributes.addFlashAttribute("mailError", "Ошибка отправки подтверждения по почте.");
                return "redirect:/visitor/editProfile";
            }

            // Создаём временного "редактируемого" пользователя с новыми данными
            Visitor editedVisitor = new Visitor(
                    originalVisitor.getId(),
                    inputLastName,
                    inputFirstName,
                    inputPatronymicName,
                    inputSex,
                    birthday,
                    username,
                    inputMobileNumber,
                    originalVisitor.getDateRegistration(),
                    originalVisitor.getRole(),
                    originalVisitor.isNeedChangePass()
            );

            request.getSession().setAttribute("visitor", editedVisitor); // заменяем на отредактированного
            request.getSession().setAttribute("oneTimePassword", oneTimePassword);
            return "redirect:/visitor/verify-code?mode=edit";
        }

        // Если username не менялся — сохраняем сразу
        boolean result = visitorService.editVisitor(originalVisitor.getId(), inputLastName, inputFirstName,
                inputPatronymicName, inputSex, birthday, username, inputMobileNumber, false);

        if (!result) {
            redirectAttributes.addFlashAttribute("visitorError", "Ошибка при сохранении изменений.");
            return "redirect:/visitor/editProfile";
        }

        return "redirect:/visitor/profile";
    }

    @GetMapping("/visitor/reset-password")
    public String resetVisitorPassword(Model model) {
        return "visitor/general/reset-password";
    }

    @PostMapping("/visitor/reset-password")
    public String resetVisitorPassword(@RequestParam String inputUserName,
                                       HttpServletRequest request,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        Optional<Visitor> visitorOptional = visitorService.getVisitorRepository().findByUsername(inputUserName);

        if (visitorOptional.isEmpty()) {
            model.addAttribute("userNameNotFound", "Пользователь не найден.");
            return "visitor/general/reset-password";
        }

        Visitor visitor = visitorOptional.get();
        String oneTimePassword = RandomPasswordGenerator.getOneTimePassword();

        request.getSession().setAttribute("visitor", visitor);
        request.getSession().setAttribute("oneTimePassword", oneTimePassword);

        boolean result = mailService.sendOneTimePasswordMail(visitor.getUsername(), oneTimePassword);
        if (!result) {
            model.addAttribute("userNameNotFound", "Ошибка при отправке письма. Повторите позже.");
            return "visitor/general/reset-password";
        }

        return "redirect:/visitor/verify-code?mode=reset";
    }



}
