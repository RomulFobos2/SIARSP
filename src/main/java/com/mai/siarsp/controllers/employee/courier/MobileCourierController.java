package com.mai.siarsp.controllers.employee.courier;

import com.mai.siarsp.dto.DeliveryTaskDTO;
import com.mai.siarsp.enumeration.DeliveryTaskStatus;
import com.mai.siarsp.models.DeliveryTask;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.service.employee.DeliveryTaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/employee/courier/deliveryTasks/mobile")
public class MobileCourierController {

    private final DeliveryTaskService deliveryTaskService;

    public MobileCourierController(DeliveryTaskService deliveryTaskService) {
        this.deliveryTaskService = deliveryTaskService;
    }

    @GetMapping("/myDeliveryTasks")
    public List<DeliveryTaskDTO> myDeliveryTasks(@AuthenticationPrincipal Employee currentUser,
                                                 @RequestParam(required = false, defaultValue = "active") String status) {
        return switch (status) {
            case "completed" -> deliveryTaskService.getTasksByDriverAndStatuses(
                    currentUser.getId(),
                    List.of(DeliveryTaskStatus.DELIVERED, DeliveryTaskStatus.CANCELLED));
            case "all" -> deliveryTaskService.getTasksByDriver(currentUser.getId());
            default -> deliveryTaskService.getTasksByDriverAndStatuses(
                    currentUser.getId(),
                    List.of(DeliveryTaskStatus.PENDING, DeliveryTaskStatus.LOADING,
                            DeliveryTaskStatus.LOADED, DeliveryTaskStatus.IN_TRANSIT));
        };
    }


    @PostMapping("/startDelivery/{id}")
    public ResponseEntity<ApiResponse> startDelivery(@AuthenticationPrincipal Employee currentUser,
                                                     @PathVariable Long id,
                                                     @RequestBody StartDeliveryRequest request) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(id);
        if (optTask.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Задача не найдена"));
        }

        DeliveryTask task = optTask.get();
        if (task.getDriver() == null || !task.getDriver().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Задача не принадлежит текущему курьеру"));
        }

        if (request.startMileage() == null) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Не указан начальный пробег"));
        }

        boolean started = deliveryTaskService.startDelivery(id, request.startMileage());
        if (!started) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Не удалось начать доставку. Проверьте статус задачи и пробег"));
        }

        return ResponseEntity.ok(new ApiResponse(true, "Доставка начата"));
    }

    @PostMapping("/markRoutePoint/{id}/{pointId}")
    public ResponseEntity<ApiResponse> markRoutePoint(@AuthenticationPrincipal Employee currentUser,
                                                       @PathVariable Long id,
                                                       @PathVariable Long pointId) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(id);
        if (optTask.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Задача не найдена"));
        }

        DeliveryTask task = optTask.get();
        if (task.getDriver() == null || !task.getDriver().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Задача не принадлежит текущему курьеру"));
        }

        boolean marked = deliveryTaskService.markRoutePointReached(id, pointId);
        if (!marked) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Не удалось отметить маршрутную точку"));
        }

        return ResponseEntity.ok(new ApiResponse(true, "Маршрутная точка отмечена"));
    }

    @PostMapping("/completeDelivery/{id}")
    public ResponseEntity<ApiResponse> completeDelivery(@AuthenticationPrincipal Employee currentUser,
                                                         @PathVariable Long id,
                                                         @RequestBody CompleteDeliveryRequest request) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(id);
        if (optTask.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Задача не найдена"));
        }

        DeliveryTask task = optTask.get();
        if (task.getDriver() == null || !task.getDriver().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Задача не принадлежит текущему курьеру"));
        }

        if (request.endMileage() == null) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Не указан конечный пробег"));
        }

        boolean completed = deliveryTaskService.completeDelivery(
                id,
                request.endMileage(),
                request.clientRepresentative(),
                request.actComment());
        if (!completed) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Не удалось завершить доставку. Проверьте маршрутные точки и статус задачи"));
        }

        return ResponseEntity.ok(new ApiResponse(true, "Доставка завершена"));
    }

    @PostMapping("/updateLocation/{id}")
    public ResponseEntity<ApiResponse> updateLocation(@AuthenticationPrincipal Employee currentUser,
                                                       @PathVariable Long id,
                                                       @RequestBody UpdateLocationRequest request) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(id);
        if (optTask.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Задача не найдена"));
        }

        DeliveryTask task = optTask.get();
        if (task.getDriver() == null || !task.getDriver().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Задача не принадлежит текущему курьеру"));
        }

        if (request.latitude() == null || request.longitude() == null) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Пустые координаты"));
        }

        boolean updated = deliveryTaskService.updateLocation(id, request.latitude(), request.longitude());
        if (!updated) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Не удалось обновить координаты. Проверьте, что задача в статусе IN_TRANSIT"));
        }

        return ResponseEntity.ok(new ApiResponse(true, "Координаты обновлены"));
    }

    public record UpdateLocationRequest(Double latitude, Double longitude) {}

    public record StartDeliveryRequest(Integer startMileage) {}

    public record CompleteDeliveryRequest(Integer endMileage, String clientRepresentative, String actComment) {}

    public record ApiResponse(boolean success, String message) {}
}
