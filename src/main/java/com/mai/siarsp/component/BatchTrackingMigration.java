package com.mai.siarsp.component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Однократная миграция при переходе на партионный учёт (FEFO).
 * <p>
 * Старая модель: «Срок годности» — атрибут Product (LocalDate-строка), ZoneProduct ссылается на Product.
 * Новая модель: «Срок годности» — Integer (число дней) атрибут Product; Supply имеет productionDate и
 * expirationDate; ZoneProduct ссылается на Supply.
 * <p>
 * Семантика значения атрибута меняется → старые LocalDate-строки удаляются. Все Supply/Delivery/ZoneProduct/
 * WriteOffAct очищаются — менеджер заново оформит приёмки. Stock-счётчики на Product обнуляются.
 * <p>
 * Маркер выполнения хранится в служебной таблице {@code t_migration_marker}.
 */
@Component
@Slf4j
public class BatchTrackingMigration {

    private static final String MARKER = "BATCH_TRACKING_V1";

    private final JdbcTemplate jdbc;

    public BatchTrackingMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    @Transactional
    public void runIfNeeded() {
        try {
            jdbc.execute("CREATE TABLE IF NOT EXISTS t_migration_marker (" +
                    "name VARCHAR(100) NOT NULL PRIMARY KEY, " +
                    "executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        } catch (DataAccessException e) {
            log.warn("Не удалось создать t_migration_marker: {}", e.getMessage());
            return;
        }

        Integer count;
        try {
            count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM t_migration_marker WHERE name = ?",
                    Integer.class, MARKER);
        } catch (DataAccessException e) {
            log.warn("Не удалось проверить маркер миграции: {}", e.getMessage());
            return;
        }
        if (count != null && count > 0) {
            return;
        }

        log.info("Запуск миграции на партионный учёт ({}): чистим Supply/Delivery/ZoneProduct/WriteOffAct " +
                "и атрибут «Срок годности»", MARKER);

        // Порядок важен: сначала зависимые таблицы.
        execIgnore("DELETE FROM t_zoneProduct");
        execIgnore("DELETE FROM t_writeOffAct");
        execIgnore("DELETE FROM t_supply");
        execIgnore("DELETE FROM t_delivery");

        execIgnore("UPDATE t_product SET stockQuantity = 0, quantityForStock = 0, reservedQuantity = 0");

        // Атрибут меняет семантику с LocalDate-строки на число дней → старые значения удаляем.
        execIgnore("DELETE FROM t_productAttributeValue WHERE attribute_id IN " +
                "(SELECT id FROM t_productAttribute WHERE name = 'Срок годности')");

        try {
            jdbc.update("INSERT INTO t_migration_marker(name) VALUES (?)", MARKER);
            log.info("Миграция {} выполнена успешно", MARKER);
        } catch (DataAccessException e) {
            log.error("Не удалось записать маркер миграции {}: {}", MARKER, e.getMessage());
        }
    }

    private void execIgnore(String sql) {
        try {
            int rows = jdbc.update(sql);
            log.info("Миграция: «{}» — затронуто {} строк", sql, rows);
        } catch (DataAccessException e) {
            log.warn("Миграция: «{}» — пропущено: {}", sql, e.getMessage());
        }
    }
}
