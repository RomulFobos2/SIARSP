package com.mai.siarsp.service.employee.requestForDelivery;

import com.mai.siarsp.enumeration.RequestStatus;
import com.mai.siarsp.models.RequestForDelivery;
import com.mai.siarsp.service.employee.NotificationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RequestForDeliveryNotificationService {

    private final NotificationService notificationService;

    public RequestForDeliveryNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void notifyStatusChanged(RequestForDelivery request, RequestStatus newStatus) {
        String text = buildText(request, newStatus);

        switch (newStatus) {
            case PENDING_DIRECTOR -> notificationService.notifyByRole("ROLE_EMPLOYEE_MANAGER", text);
            case REJECTED_BY_DIRECTOR -> notificationService.notifyByRole("ROLE_EMPLOYEE_WAREHOUSE_MANAGER", text);
            case PENDING_ACCOUNTANT -> notificationService.notifyByRole("ROLE_EMPLOYEE_ACCOUNTER", text);
            case REJECTED_BY_ACCOUNTANT -> notificationService.notifyByRoles(
                    List.of("ROLE_EMPLOYEE_MANAGER", "ROLE_EMPLOYEE_WAREHOUSE_MANAGER"), text);
            case APPROVED -> notificationService.notifyByRoles(
                    List.of("ROLE_EMPLOYEE_MANAGER", "ROLE_EMPLOYEE_WAREHOUSE_MANAGER"), text);
            case PARTIALLY_RECEIVED, RECEIVED -> notificationService.notifyByRoles(
                    List.of("ROLE_EMPLOYEE_MANAGER", "ROLE_EMPLOYEE_ACCOUNTER"), text);
            default -> {
            }
        }
    }

    private String buildText(RequestForDelivery request, RequestStatus status) {
        return switch (status) {
            case PENDING_DIRECTOR -> String.format(
                    "Заявка на поставку №%d от %s для поставщика «%s» отправлена на согласование",
                    request.getId(), request.getRequestDate(), request.getSupplier().getName());
            case REJECTED_BY_DIRECTOR -> String.format(
                    "Заявка на поставку №%d от %s для поставщика «%s» отклонена директором",
                    request.getId(), request.getRequestDate(), request.getSupplier().getName());
            case PENDING_ACCOUNTANT -> String.format(
                    "Заявка на поставку №%d от %s для поставщика «%s» согласована директором, ожидает согласования бухгалтера",
                    request.getId(), request.getRequestDate(), request.getSupplier().getName());
            case REJECTED_BY_ACCOUNTANT -> String.format(
                    "Заявка на поставку №%d от %s для поставщика «%s» отклонена бухгалтером",
                    request.getId(), request.getRequestDate(), request.getSupplier().getName());
            case APPROVED -> String.format(
                    "Заявка на поставку №%d от %s для поставщика «%s» полностью согласована",
                    request.getId(), request.getRequestDate(), request.getSupplier().getName());
            case PARTIALLY_RECEIVED -> String.format(
                    "Заявка на поставку №%d от %s для поставщика «%s» частично выполнена",
                    request.getId(), request.getRequestDate(), request.getSupplier().getName());
            case RECEIVED -> String.format(
                    "Заявка на поставку №%d от %s для поставщика «%s» полностью выполнена",
                    request.getId(), request.getRequestDate(), request.getSupplier().getName());
            default -> "";
        };
    }
}
