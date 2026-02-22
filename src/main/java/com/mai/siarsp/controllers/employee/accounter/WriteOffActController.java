package com.mai.siarsp.controllers.employee.accounter;

import com.mai.siarsp.dto.WriteOffActDTO;
import com.mai.siarsp.models.WriteOffAct;
import com.mai.siarsp.service.employee.WriteOffActService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

/**
 * Read-only контроллер актов списания для бухгалтера (ACCOUNTER).
 * Позволяет только просматривать акты списания для учёта.
 */
@Controller("accounterWriteOffActController")
@RequestMapping("/employee/accounter/writeOffActs")
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
        return "employee/accounter/writeOffActs/allWriteOffActs";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsWriteOffAct/{id}")
    public String detailsWriteOffAct(@PathVariable Long id, Model model) {
        Optional<WriteOffAct> optAct = writeOffActService.getActById(id);
        if (optAct.isEmpty()) {
            return "redirect:/employee/accounter/writeOffActs/allWriteOffActs";
        }
        model.addAttribute("act", optAct.get());
        return "employee/accounter/writeOffActs/detailsWriteOffAct";
    }
}
