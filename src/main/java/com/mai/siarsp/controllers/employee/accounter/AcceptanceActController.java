package com.mai.siarsp.controllers.employee.accounter;

import com.mai.siarsp.models.AcceptanceAct;
import com.mai.siarsp.service.employee.AcceptanceActService;
import com.mai.siarsp.service.general.AcceptanceActDocumentService;
import com.mai.siarsp.service.general.ReportDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller("accounterAcceptanceActController")
@RequestMapping("/employee/accounter/acceptanceActs")
@Slf4j
public class AcceptanceActController {

    private static final String ROLE_PATH = "accounter";
    private final AcceptanceActService acceptanceActService;

    public AcceptanceActController(AcceptanceActService acceptanceActService) {
        this.acceptanceActService = acceptanceActService;
    }

    @Transactional(readOnly = true)
    @GetMapping("/allAcceptanceActs")
    public String allAcceptanceActs(@RequestParam(required = false, defaultValue = "all") String status,
                                     Model model) {
        List<AcceptanceAct> acts;
        switch (status) {
            case "signed" -> acts = acceptanceActService.getActsByStatus(true);
            case "unsigned" -> acts = acceptanceActService.getActsByStatus(false);
            default -> {
                acts = acceptanceActService.getAllActs();
                status = "all";
            }
        }
        model.addAttribute("acts", acts);
        model.addAttribute("currentStatus", status);
        model.addAttribute("rolePath", ROLE_PATH);
        return "employee/" + ROLE_PATH + "/acceptanceActs/allAcceptanceActs";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsAcceptanceAct/{id}")
    public String detailsAcceptanceAct(@PathVariable Long id, Model model) {
        Optional<AcceptanceAct> opt = acceptanceActService.getActById(id);
        if (opt.isEmpty()) {
            return "redirect:/employee/" + ROLE_PATH + "/acceptanceActs/allAcceptanceActs";
        }
        AcceptanceAct act = opt.get();
        model.addAttribute("act", act);
        model.addAttribute("order", act.getClientOrder());
        model.addAttribute("canEdit", false);
        model.addAttribute("backUrl", "/employee/" + ROLE_PATH + "/acceptanceActs/allAcceptanceActs");
        model.addAttribute("downloadUrl", "/employee/" + ROLE_PATH + "/acceptanceActs/downloadAcceptanceAct/" + id);
        return "employee/warehouseManager/documents/detailsAcceptanceAct";
    }

    @Transactional(readOnly = true)
    @GetMapping("/downloadAcceptanceAct/{id}")
    public ResponseEntity<byte[]> downloadAcceptanceAct(@PathVariable Long id) throws IOException {
        Optional<AcceptanceAct> opt = acceptanceActService.getActById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ReportDocumentService.ReportFile file = AcceptanceActDocumentService.generateDocument(opt.get());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(file.fileName()).build());
        return ResponseEntity.ok().headers(headers).body(file.content());
    }
}
