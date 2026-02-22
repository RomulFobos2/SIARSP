package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.WriteOffActStatus;
import com.mai.siarsp.enumeration.WriteOffReason;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Акт списания товара
 *
 * Документ, оформляющий выбытие товара со склада по причинам,
 * не связанным с продажей клиентам (брак, порча, истечение срока годности и т.д.).
 *
 *
 * Причины списания (см. WriteOffReason enum):
 * - DEFECT - производственный брак (дефект при производстве)
 * - EXPIRED - истек срок годности (товар просрочен)
 * - DAMAGE - повреждение (при хранении или транспортировке)
 * - LOSS - утрата (потеря, кража, недостача при инвентаризации)
 * - OTHER - другие причины (любая другая причина списания)
 *
 * Влияние на складские остатки:
 * При создании и утверждении акта списания:
 * - Product.stockQuantity уменьшается на quantity
 * - Если товар был размещен на полках:
 *   - ZoneProduct.quantity уменьшается или запись удаляется
 * - Данные об убытках передаются в бухгалтерию
 *
 * Связи:
 * - Относится к одному товару (Product)
 * - Подписывается ответственным сотрудником (Employee)
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_writeOffAct")
@EqualsAndHashCode(of = "id")
public class WriteOffAct {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Номер акта списания
     * Уникальный номер документа для учета и идентификации
     *
     * Формат: обычно АС-YYYYMMDD-NNNN
     * Уникален в системе для предотвращения дублирования документов
     */
    @Column(nullable = false, unique = true, length = 50)
    private String actNumber;

    /**
     * Дата составления акта списания
     * Дата, когда был выявлен факт необходимости списания
     * и оформлен документ
     *
     * Обычно совпадает с текущей датой, но может быть указана
     * другая дата (например, дата обнаружения брака)
     *
     * Автоматически устанавливается на текущую дату при создании
     */
    @Column(nullable = false)
    private LocalDate actDate;

    /**
     * Причина списания товара
     *
     * Возможные причины (см. WriteOffReason enum):
     * - DEFECT - производственный брак
     *   Пример: Товар с браком от поставщика, дефект упаковки
     *
     * - EXPIRED - истек срок годности
     *   Пример: Молоко просрочено, срок годности вышел
     *
     * - DAMAGE - повреждение
     *   Пример: Повреждение при хранении, транспортировке, падении
     *
     * - LOSS - утрата, недостача
     *   Пример: Потеря, кража, недостача при инвентаризации
     *
     * - OTHER - другие причины
     *   Пример: Любые другие причины, не попадающие в основные категории
     *
     * Примеры анализа:
     * - Много EXPIRED → улучшить прогнозирование спроса
     * - Много DEFECT → сменить поставщика или ужесточить приемку
     * - Много DAMAGE → улучшить условия хранения
     * - Много LOSS → проверить безопасность, усилить контроль
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WriteOffReason reason;

    /**
     * Количество списываемого товара
     * Сколько единиц товара подлежит списанию
     *
     * Важно:
     * При утверждении акта система должна проверить:
     * - Product.stockQuantity >= quantity (достаточно товара для списания)
     * - Если недостаточно → ошибка или предупреждение
     *
     * После списания:
     * - Product.stockQuantity уменьшается на quantity
     * - Если товар был на полках → ZoneProduct обновляется
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * Статус акта списания
     *
     * Жизненный цикл:
     * - PENDING_DIRECTOR — на подписи у директора (после создания)
     * - APPROVED — утверждён директором (товар списан)
     * - REJECTED — отклонён директором
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WriteOffActStatus status = WriteOffActStatus.PENDING_DIRECTOR;

    /**
     * Комментарий к акту списания
     * Подробное описание обстоятельств и причин списания
     *
     */
    @Column(length = 500)
    private String comment;

    /**
     * Комментарий директора при отклонении акта
     * Заполняется при переводе акта в статус REJECTED
     */
    @Column(length = 500)
    private String directorComment;

    /**
     * Товар, подлежащий списанию
     * Ссылка на справочник товаров
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Product product;

    /**
     * Ответственный сотрудник
     * Сотрудник, составивший и подписавший акт списания
     *
     * Обычно это заведующий складом (ROLE_WAREHOUSE_MANAGER),
     * так как он отвечает за сохранность товара на складе
     *
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Employee responsibleEmployee;

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создает новый акт списания товара
     * Автоматически устанавливает текущую дату как дату акта
     *
     * @param actNumber уникальный номер акта (генерируется сервисом)
     * @param product товар, подлежащий списанию
     * @param quantity количество единиц товара для списания
     * @param reason причина списания (DEFECT, EXPIRED, DAMAGE, LOSS, OTHER)
     * @param responsibleEmployee сотрудник, составивший акт
     */
    public WriteOffAct(String actNumber, Product product, int quantity,
                       WriteOffReason reason, Employee responsibleEmployee) {
        this.actNumber = actNumber;
        this.product = product;
        this.quantity = quantity;
        this.reason = reason;
        this.responsibleEmployee = responsibleEmployee;
        this.actDate = LocalDate.now();
    }

    // ========== МЕТОДЫ ==========
    // Специфичных методов нет, используются стандартные getter/setter от Lombok
    //
    // Возможные методы для добавления в сервисный слой:
    // - calculateLossAmount() - расчет суммы убытка (quantity × закупочная_цена)
    // - approve() - утверждение акта и списание товара со склада
    // - generateActDocument() - формирование печатной формы акта
    // - validateQuantity() - проверка достаточности товара для списания

}