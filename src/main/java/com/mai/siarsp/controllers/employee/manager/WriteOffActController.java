package com.mai.siarsp.controllers.employee.manager;

import com.mai.siarsp.dto.WriteOffActDTO;
import com.mai.siarsp.models.WriteOffAct;
import com.mai.siarsp.service.employee.WriteOffActService;
import com.mai.siarsp.service.general.ReportDocumentService;
import com.mai.siarsp.service.general.WriteOffActDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Контроллер актов списания для директора (MANAGER).
 * Позволяет просматривать, утверждать и отклонять акты списания.
 */
@Controller("managerWriteOffActController")
@RequestMapping("/employee/manager/writeOffActs")
@Slf4j
public class WriteOffActController {

    private final WriteOffActService writeOffActService;

    public WriteOffActController(WriteOffActService writeOffActService) {
        this.writeOffActService = writeOffActService;
    }

    @GetMapping("/allWriteOffActs")
    public String allWriteOffActs(Model model) {
        List<WriteOffActDTO> acts = writeOffActService.getAllActs();
        model.addAttribute("acts", acts);
        return "employee/manager/writeOffActs/allWriteOffActs";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsWriteOffAct/{id}")
    public String detailsWriteOffAct(@PathVariable Long id, Model model) {
        Optional<WriteOffAct> optAct = writeOffActService.getActById(id);
        if (optAct.isEmpty()) {
            return "redirect:/employee/manager/writeOffActs/allWriteOffActs";
        }
        model.addAttribute("act", optAct.get());
        return "employee/manager/writeOffActs/detailsWriteOffAct";
    }

    @PostMapping("/approveWriteOffAct/{id}")
    public String approveWriteOffAct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean success = writeOffActService.approveAct(id);

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Акт списания утверждён. Товар списан со склада.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при утверждении акта. Возможно, недостаточно товара на складе.");
        }
        return "redirect:/employee/manager/writeOffActs/detailsWriteOffAct/" + id;
    }

    @PostMapping("/rejectWriteOffAct/{id}")
    public String rejectWriteOffAct(@PathVariable Long id,
                                    @RequestParam(required = false) String directorComment,
                                    RedirectAttributes redirectAttributes) {
        boolean success = writeOffActService.rejectAct(id, directorComment);

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Акт списания отклонён.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при отклонении акта.");
        }
        return "redirect:/employee/manager/writeOffActs/detailsWriteOffAct/" + id;
    }

    @Transactional(readOnly = true)
    @GetMapping("/downloadWriteOffAct/{id}")
    public ResponseEntity<byte[]> downloadWriteOffAct(@PathVariable Long id) throws IOException {
        Optional<WriteOffAct> optAct = writeOffActService.getActById(id);
        if (optAct.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ReportDocumentService.ReportFile file = WriteOffActDocumentService.generateDocument(optAct.get());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(file.content());
    }
}
