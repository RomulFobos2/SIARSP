package com.mai.siarsp.controllers.visitor;

import com.mai.siarsp.component.RandomPasswordGenerator;
import com.mai.siarsp.models.Visitor;
import com.mai.siarsp.service.general.MailService;
import com.mai.siarsp.service.visitor.VisitorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

@Controller
@Slf4j
//TODO Добавить обработку ошибки в случе, если письмо не смогло отправиться. Типа сервис не доступен повторите позже.
public class VisitorRegistrationController {
    private final VisitorService visitorService;
    private final MailService mailService;

    public VisitorRegistrationController(VisitorService visitorService, MailService mailService) {
        this.visitorService = visitorService;
        this.mailService = mailService;
    }

    @GetMapping("/visitor/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        log.info("Проверка имени пользователя {}.", username);
        boolean exists = visitorService.checkUserName(username);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/visitor/registration")
    public String visitorRegistration(Model model){
        LocalDate maxDate = LocalDate.now().minusYears(14);
        model.addAttribute("maxDate", maxDate.toString());
        return "visitor/general/registration";
    }


    @PostMapping("/visitor/registration")
    public String registerVisitor(@RequestParam String inputLastName,
                                  @RequestParam String inputFirstName,
                                  @RequestParam String inputPatronymicName,
                                  @RequestParam String inputSex,
                                  @RequestParam LocalDate birthday,
                                  @RequestParam String username,
                                  @RequestParam String password,
                                  @RequestParam String passwordConfirm,
                                  @RequestParam String inputMobileNumber,
                                  Model model,
                                  HttpServletRequest request) {

        Visitor visitor = new Visitor(inputLastName, inputFirstName, inputPatronymicName, inputSex, birthday, username, password, passwordConfirm, inputMobileNumber);

        //TODO на страницу добавить js для проверки пароля и подверждения пароля

        String oneTimePassword = RandomPasswordGenerator.getOneTimePassword();
        if (!mailService.sendOneTimePasswordMail(username, oneTimePassword)) {
            model.addAttribute("mailError", "Не удалось отправить письмо. Повторите позже.");
            return "visitor/general/registration";
        }

        request.getSession().setAttribute("visitor", visitor);
        request.getSession().setAttribute("oneTimePassword", oneTimePassword);
        return "redirect:/visitor/verify-code?mode=activate";

    }

    @GetMapping("/visitor/verify-code")
    public String showVerificationPage(@RequestParam String mode, Model model, HttpServletRequest request) {
        Visitor visitor = (Visitor) request.getSession().getAttribute("visitor");
        model.addAttribute("visitor", visitor);

        log.info("mode в  showVerificationPage - " + mode);

        switch (mode) {
            case "activate" -> {
                model.addAttribute("formAction", "/visitor/verify-code/activate");
                model.addAttribute("formTitle", "Пожалуйста, введите код для активации аккаунта");
            }
            case "edit" -> {
                model.addAttribute("formAction", "/visitor/verify-code/edit");
                model.addAttribute("formTitle", "Пожалуйста, введите код для подтверждения изменений");
            }
            case "reset" -> {
                model.addAttribute("formAction", "/visitor/verify-code/reset");
                model.addAttribute("formTitle", "Пожалуйста, введите код для сброса пароля");
            }
            default -> {
                return "redirect:/";
            }
        }

        return "visitor/general/verify-code";
    }


    @PostMapping("/visitor/verify-code/activate")
    public String handleActivation(@RequestParam String code, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Visitor visitor = (Visitor) request.getSession().getAttribute("visitor");
        String sessionCode = (String) request.getSession().getAttribute("oneTimePassword");

        if (sessionCode != null && sessionCode.equals(code)) {
            visitorService.saveVisitor(visitor);
            return "redirect:/visitor/login";
        } else {
            redirectAttributes.addFlashAttribute("codeError", "Неверно введен код подтверждения");
            return "redirect:/visitor/verify-code?mode=activate";
        }
    }


    @PostMapping("/visitor/verify-code/edit")
    public String handleEditConfirm(@RequestParam String code, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Visitor visitor = (Visitor) request.getSession().getAttribute("visitor");
        String sessionCode = (String) request.getSession().getAttribute("oneTimePassword");

        if (sessionCode != null && sessionCode.equals(code)) {
            boolean result = visitorService.editVisitor(
                    visitor.getId(),
                    visitor.getLastName(),
                    visitor.getFirstName(),
                    visitor.getPatronymicName(),
                    visitor.getSex(),
                    visitor.getDateBirthday(),
                    visitor.getUsername(),
                    visitor.getMobileNumber(),
                    true
            );

            if (!result) {
                redirectAttributes.addFlashAttribute("codeError", "Ошибка при сохранении изменений.");
                return "redirect:/visitor/verify-code?mode=edit";
            }

            return "redirect:/logout";
        } else {
            redirectAttributes.addFlashAttribute("codeError", "Неверно введен код подтверждения");
            return "redirect:/visitor/verify-code?mode=edit";
        }
    }

    @PostMapping("/visitor/verify-code/reset")
    public String handleResetPassword(@RequestParam String code, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Visitor visitor = (Visitor) request.getSession().getAttribute("visitor");
        String sessionCode = (String) request.getSession().getAttribute("oneTimePassword");

        if (visitor == null || sessionCode == null || !sessionCode.equals(code)) {
            redirectAttributes.addFlashAttribute("codeError", "Неверно введен код подтверждения");
            return "redirect:/visitor/verify-code?mode=reset";
        }

        String newPassword = RandomPasswordGenerator.getOneTimePassword();
        boolean result = visitorService.resetVisitorPassword(visitor.getId(), newPassword);

        if (!result) {
            redirectAttributes.addFlashAttribute("codeError", "Ошибка при сбросе пароля. Попробуйте позже.");
            return "redirect:/visitor/verify-code?mode=reset";
        }

        mailService.sendResetPasswordMail(visitor.getUsername(), newPassword);
        redirectAttributes.addFlashAttribute("resetSuccess", "Пароль был сброшен. Новый пароль отправлен на почту.");
        return "redirect:/visitor/login";
    }


    @PostMapping("/visitor/verify-code/resend-code")
    public String resendCode(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Visitor visitor = (Visitor) request.getSession().getAttribute("visitor");
        String mode = request.getParameter("mode");



        if (visitor == null || mode == null) {
            redirectAttributes.addFlashAttribute("codeError", "Ошибка: данные для подтверждения не найдены.");
            return "redirect:/visitor/login";
        }

        String oneTimePassword = RandomPasswordGenerator.getOneTimePassword();
        request.getSession().setAttribute("oneTimePassword", oneTimePassword);

        boolean sent = mailService.sendOneTimePasswordMail(visitor.getUsername(), oneTimePassword);

        if (!sent) {
            redirectAttributes.addFlashAttribute("codeError", "Ошибка отправки письма. Попробуйте позже.");
        } else {
            redirectAttributes.addFlashAttribute("codeError", "Код повторно отправлен на почту.");
        }

        return "redirect:/visitor/verify-code?mode=" + mode;
    }


}
