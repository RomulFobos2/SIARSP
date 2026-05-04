package com.mai.siarsp.controllers.employee.accounter;

import java.nio.charset.StandardCharsets;
import com.mai.siarsp.models.AcceptanceAct;
import com.mai.siarsp.models.DeliveryTask;
import com.mai.siarsp.service.employee.DeliveryTaskService;
import com.mai.siarsp.service.general.AcceptanceActDocumentService;
import com.mai.siarsp.service.general.ContractService;
import com.mai.siarsp.service.general.ReportDocumentService;
import com.mai.siarsp.service.general.TTNDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
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
import java.util.Optional;

@Controller("accounterDeliveryTaskController")
@RequestMapping("/employee/accounter/deliveryTasks")
@Slf4j
public class DeliveryTaskController {

    private final DeliveryTaskService deliveryTaskService;

    public DeliveryTaskController(DeliveryTaskService deliveryTaskService) {
        this.deliveryTaskService = deliveryTaskService;
    }

    @GetMapping("/allDeliveryTasks")
    public String allDeliveryTasks(Model model) {
        model.addAttribute("tasks", deliveryTaskService.getAllTasks());
        return "employee/accounter/deliveryTasks/allDeliveryTasks";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsDeliveryTask/{id}")
    public String detailsDeliveryTask(@PathVariable Long id, Model model) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(id);
        if (optTask.isEmpty()) {
            return "redirect:/employee/accounter/deliveryTasks/allDeliveryTasks";
        }

        model.addAttribute("task", optTask.get());
        return "employee/accounter/deliveryTasks/detailsDeliveryTask";
    }

    @Transactional(readOnly = true)
    @GetMapping("/createTTN/{taskId}")
    public String createTTNForm(@PathVariable Long taskId, Model model) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(taskId);
        if (optTask.isEmpty()) {
            return "redirect:/employee/accounter/deliveryTasks/allDeliveryTasks";
        }

        model.addAttribute("task", optTask.get());
        return "employee/accounter/deliveryTasks/createTTN";
    }

    @PostMapping("/createTTN/{taskId}")
    public String createTTN(@PathVariable Long taskId,
                            @RequestParam(required = false) String cargoDescription,
                            @RequestParam(required = false) Double totalWeight,
                            @RequestParam(required = false) Double totalVolume,
                            @RequestParam(required = false) String comment,
                            RedirectAttributes redirectAttributes) {
        if (!deliveryTaskService.createTTN(taskId, cargoDescription, totalWeight, totalVolume, comment)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при оформлении ТТН.");
            return "redirect:/employee/accounter/deliveryTasks/createTTN/" + taskId;
        }

        redirectAttributes.addFlashAttribute("successMessage", "ТТН успешно оформлена.");
        return "redirect:/employee/accounter/deliveryTasks/detailsDeliveryTask/" + taskId;
    }

    // ========== СКАЧИВАНИЕ КОНТРАКТА ==========

    @GetMapping("/downloadContract/{taskId}")
    public ResponseEntity<Resource> downloadContract(@PathVariable Long taskId) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(taskId);
        if (optTask.isEmpty() || optTask.get().getClientOrder() == null
                || optTask.get().getClientOrder().getContractFile() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            String contractFileName = optTask.get().getClientOrder().getContractFile();
            Resource resource = ContractService.getContractData(contractFileName);
            String downloadName = contractFileName.contains("_")
                    ? contractFileName.substring(contractFileName.indexOf("_") + 1)
                    : contractFileName;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(downloadName, StandardCharsets.UTF_8)
                    .build());
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (IOException e) {
            log.error("Ошибка скачивания контракта: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // ========== ПРОСМОТР ДОКУМЕНТОВ (read-only) ==========

    @Transactional(readOnly = true)
    @GetMapping("/detailsTTN/{taskId}")
    public String detailsTTN(@PathVariable Long taskId, Model model) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(taskId);
        if (optTask.isEmpty() || optTask.get().getTtn() == null) {
            return "redirect:/employee/accounter/deliveryTasks/detailsDeliveryTask/" + taskId;
        }
        DeliveryTask task = optTask.get();
        model.addAttribute("order", task.getClientOrder());
        model.addAttribute("ttn", task.getTtn());
        model.addAttribute("canEdit", false);
        model.addAttribute("backUrl", "/employee/accounter/deliveryTasks/detailsDeliveryTask/" + taskId);
        model.addAttribute("downloadUrl", "/employee/accounter/deliveryTasks/downloadTTN/" + taskId);
        return "employee/warehouseManager/documents/detailsTTN";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsAcceptanceAct/{taskId}")
    public String detailsAcceptanceAct(@PathVariable Long taskId, Model model) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(taskId);
        if (optTask.isEmpty() || optTask.get().getClientOrder() == null) {
            return "redirect:/employee/accounter/deliveryTasks/detailsDeliveryTask/" + taskId;
        }
        DeliveryTask task = optTask.get();
        Optional<AcceptanceAct> optAct = deliveryTaskService.getAcceptanceActByOrder(task.getClientOrder().getId());
        if (optAct.isEmpty()) {
            return "redirect:/employee/accounter/deliveryTasks/detailsDeliveryTask/" + taskId;
        }
        model.addAttribute("order", task.getClientOrder());
        model.addAttribute("act", optAct.get());
        model.addAttribute("canEdit", false);
        model.addAttribute("backUrl", "/employee/accounter/deliveryTasks/detailsDeliveryTask/" + taskId);
        model.addAttribute("downloadUrl", "/employee/accounter/deliveryTasks/downloadAcceptanceAct/" + taskId);
        return "employee/warehouseManager/documents/detailsAcceptanceAct";
    }

    // ========== СКАЧИВАНИЕ ДОКУМЕНТОВ (ТТН + Акт) ==========

    @Transactional(readOnly = true)
    @GetMapping("/downloadTTN/{taskId}")
    public ResponseEntity<byte[]> downloadTTN(@PathVariable Long taskId) throws IOException {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(taskId);
        if (optTask.isEmpty() || optTask.get().getTtn() == null) {
            return ResponseEntity.notFound().build();
        }
        ReportDocumentService.ReportFile file = TTNDocumentService.generateDocument(optTask.get().getTtn());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(file.content());
    }

    @Transactional(readOnly = true)
    @GetMapping("/downloadAcceptanceAct/{taskId}")
    public ResponseEntity<byte[]> downloadAcceptanceAct(@PathVariable Long taskId) throws IOException {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(taskId);
        if (optTask.isEmpty() || optTask.get().getClientOrder() == null) {
            return ResponseEntity.notFound().build();
        }
        Optional<AcceptanceAct> optAct = deliveryTaskService.getAcceptanceActByOrder(
                optTask.get().getClientOrder().getId());
        if (optAct.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ReportDocumentService.ReportFile file = AcceptanceActDocumentService.generateDocument(optAct.get());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(file.content());
    }
}
