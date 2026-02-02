package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.WriteOffReason;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

//TODO Пока оставлю, но скорее всего уберу. Акты будут просто формироваться на основе шаблона ворд документа и как файл хранится в хранилище, а в заказе будет поле на шаблон этого акта.
//TODO нужно подумать грузить пдф подписанный или что? ну разберемся
/**
 * Приемо-сдаточный акт
 *
 * Документ, подтверждающий передачу товара от поставщика конечному потребителю.
 * Составляется при доставке заказа клиенту. Подписывается представителем клиента
 * и водителем-экспедитором. Является одним из выходных документов системы согласно ТЗ.
 *
 * Связи:
 * - Один акт соответствует одному заказу клиента (ClientOrder)
 * - Принимает клиент (Client)
 * - Передает водитель-экспедитор (Employee)
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_acceptanceAct")
@EqualsAndHashCode(of = "id")
public class AcceptanceAct {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Номер акта приема-передачи
     * Уникальный номер документа, генерируется автоматически
     * Формат: АПП-YYYYMMDD-NNNN (например, АПП-20250203-0001)
     */
    @Column(nullable = false, unique = true, length = 50)
    private String actNumber;

    /**
     * Дата составления акта
     * Устанавливается автоматически при создании
     */
    @Column(nullable = false)
    private LocalDate actDate;

    /**
     * ФИО представителя клиента, принявшего товар
     * Заполняется водителем при подписании акта
     * Например: "Иванова Мария Петровна, заведующая складом"
     */
    @Column(length = 200)
    private String clientRepresentative;

    /**
     * Признак подписания акта
     * true - акт подписан клиентом
     * false - акт еще не подписан (в процессе доставки)
     */
    @Column(nullable = false)
    private boolean signed;

    /**
     * Дата и время фактического подписания акта
     * Заполняется автоматически при вызове метода markAsSigned()
     */
    @Column
    private LocalDateTime signedAt;

    /**
     * Комментарий к акту
     * Может содержать примечания, особые условия приемки,
     * претензии клиента и т.п.
     */
    @Column(length = 500)
    private String comment;

    /**
     * Связь с заказом клиента
     * Один акт соответствует одному заказу
     */
    @ToString.Exclude
    @OneToOne
    @JoinColumn(nullable = false)
    private ClientOrder clientOrder;

    /**
     * Клиент, принимающий товар
     * Организация-получатель (детский сад, школа, больница)
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Client client;

    /**
     * Сотрудник, передавший товар (водитель-экспедитор)
     * Ответственное лицо со стороны ИП "Левчук"
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Employee deliveredBy;

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создает новый приемо-сдаточный акт
     *
     * @param actNumber номер акта (генерируется автоматически сервисом)
     * @param clientOrder заказ клиента, для которого составляется акт
     * @param client клиент-получатель товара
     * @param deliveredBy водитель-экспедитор, передающий товар
     */
    public AcceptanceAct(String actNumber, ClientOrder clientOrder,
                         Client client, Employee deliveredBy) {
        this.actNumber = actNumber;
        this.clientOrder = clientOrder;
        this.client = client;
        this.deliveredBy = deliveredBy;
        this.actDate = LocalDate.now();
        this.signed = false;
    }

    // ========== МЕТОДЫ ==========

    /**
     * Отмечает акт как подписанный
     * Устанавливает флаг signed = true, фиксирует время подписания
     * и сохраняет ФИО представителя клиента
     *
     * @param clientRepresentative ФИО и должность представителя клиента,
     *                            принявшего товар (например, "Иванова М.П., заведующая")
     */
    public void markAsSigned(String clientRepresentative) {
        this.signed = true;
        this.signedAt = LocalDateTime.now();
        this.clientRepresentative = clientRepresentative;
    }

}