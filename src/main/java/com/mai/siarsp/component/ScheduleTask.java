package com.mai.siarsp.component;

import com.mai.siarsp.enumeration.EquipmentStatus;
import com.mai.siarsp.models.WarehouseEquipment;
import com.mai.siarsp.repo.WarehouseEquipmentRepository;
import com.mai.siarsp.service.employee.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Slf4j
public class ScheduleTask {

    private static final String MANAGER_ROLE = "ROLE_EMPLOYEE_MANAGER";

    private final WarehouseEquipmentRepository warehouseEquipmentRepository;
    private final NotificationService notificationService;

    public ScheduleTask(WarehouseEquipmentRepository warehouseEquipmentRepository,
                        NotificationService notificationService) {
        this.warehouseEquipmentRepository = warehouseEquipmentRepository;
        this.notificationService = notificationService;
    }

    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> {
        };
    }

    // Запускаем метод при старте программы и затем каждый день в полночь
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanUpDirectories() {
    }

    /**
     * Ежедневная проверка сроков службы оборудования склада.
     * Запускается каждый день в 8:00.
     *
     * Отправляет внутренние уведомления всем руководителям (ROLE_EMPLOYEE_MANAGER) о:
     * - оборудовании с истёкшим сроком службы
     * - оборудовании, срок которого заканчивается через неделю (≤7 дней)
     * - оборудовании, срок которого заканчивается через месяц (≤30 дней)
     *
     * Списанное оборудование (WRITTEN_OFF) из проверки исключается.
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkEquipmentExpiration() {
        log.info("Запуск проверки сроков службы оборудования склада");

        List<WarehouseEquipment> activeEquipment =
                warehouseEquipmentRepository.findByStatusNot(EquipmentStatus.WRITTEN_OFF);

        LocalDate today = LocalDate.now();
        int notifiedCount = 0;

        for (WarehouseEquipment eq : activeEquipment) {
            LocalDate expDate = eq.getExpirationDate();
            if (expDate == null) {
                continue;
            }

            long daysLeft = ChronoUnit.DAYS.between(today, expDate);
            String warehouseName = eq.getWarehouse() != null ? eq.getWarehouse().getName() : "—";
            String text = null;

            if (daysLeft < 0) {
                text = "⚠️ Срок службы истёк: «" + eq.getName() + "» (склад: " + warehouseName
                        + "). Дата окончания: " + expDate + ". Статус: "
                        + eq.getStatus().getDisplayName() + ".";
            } else if (daysLeft <= 7) {
                text = "⚠️ Срок службы заканчивается через " + daysLeft + " дн.: «"
                        + eq.getName() + "» (склад: " + warehouseName + ", до " + expDate + ").";
            } else if (daysLeft <= 30) {
                text = "ℹ️ Срок службы заканчивается через месяц (" + daysLeft + " дн.): «"
                        + eq.getName() + "» (склад: " + warehouseName + ", до " + expDate + ").";
            }

            if (text != null) {
                notificationService.notifyByRole(MANAGER_ROLE, text);
                notifiedCount++;
            }
        }

        log.info("Проверка сроков завершена. Отправлено уведомлений по {} единицам оборудования", notifiedCount);
    }
}
