package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.RequestStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Заявка на поставку товара от поставщика
 *
 * Документ, инициирующий процесс закупки товара у поставщика.
 * Создается заведующим складом при выявлении нехватки товара.
 * Проходит workflow согласования: Директор → Бухгалтер → отправка поставщику.
 *
 * Бизнес-процесс (согласно ТЗ):
 *
 * 1. СОЗДАНИЕ (статус DRAFT):
 *    - Заведующий складом выявляет нехватку товара (ТЗ п.177)
 *    - Создает заявку, добавляет позиции товаров (RequestedProduct)
 *    - Сохраняет как черновик
 *
 * 2. СОГЛАСОВАНИЕ ДИРЕКТОРОМ (PENDING_DIRECTOR → APPROVED / REJECTED_BY_DIRECTOR):
 *    - Заведующий отправляет на согласование директору
 *    - Директор проверяет целесообразность закупки, бюджет
 *    - Согласовывает или отклоняет с комментарием
 *
 * 3. СОГЛАСОВАНИЕ БУХГАЛТЕРОМ (PENDING_ACCOUNTANT → APPROVED / REJECTED_BY_ACCOUNTANT):
 *    - После одобрения директором заявка идет к бухгалтеру
 *    - Бухгалтер проверяет финансовую возможность, условия договора
 *    - Оформляет договор на закупку
 *    - Согласовывает или отклоняет
 *
 * 4. ОТПРАВКА ПОСТАВЩИКУ (APPROVED):
 *    - После двойного согласования заявка отправляется поставщику
 *
 * 5. ПОЛУЧЕНИЕ ТОВАРА (PARTIALLY_RECEIVED / RECEIVED):
 *    - Поставщик привозит товар → создается Delivery
 *    - Заведующий принимает товар, проверяет по накладной (ТЗ п.83-89)
 *    - При полном получении статус → RECEIVED
 *    - При частичном получении → PARTIALLY_RECEIVED
 *
 * Переходы статусов:
 * DRAFT → PENDING_DIRECTOR → PENDING_ACCOUNTANT → APPROVED → RECEIVED
 *     ↓           ↓                    ↓               ↓
 * CANCELLED  REJECTED_BY_DIRECTOR  REJECTED_BY_ACCOUNTANT  PARTIALLY_RECEIVED
 *
 * Связи:
 * - Адресована одному поставщику (Supplier)
 * - Содержит множество позиций товаров (RequestedProduct)
 * - Может быть связана с одной поставкой (Delivery) после выполнения
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_requestForDelivery")
@EqualsAndHashCode(of = "id")
public class RequestForDelivery {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Дата создания заявки
     * Автоматически устанавливается на текущую дату при создании
     *
     */
    @Column(nullable = false)
    private LocalDate requestDate = LocalDate.now();

    /**
     * Дата фактического получения товара
     * Заполняется заведующим складом после приемки товара от поставщика
     *
     * null - товар еще не получен или заявка отклонена/отменена
     */
    private LocalDate receivedDate;

    /**
     * Статус заявки в workflow согласования и выполнения
     *
     * Возможные статусы (см. RequestStatus enum):
     * - DRAFT - черновик (создан заведующим, не отправлен на согласование)
     * - PENDING_DIRECTOR - ожидает согласования директора
     * - REJECTED_BY_DIRECTOR - отклонен директором (нецелесообразная закупка)
     * - PENDING_ACCOUNTANT - ожидает согласования бухгалтера
     * - REJECTED_BY_ACCOUNTANT - отклонен бухгалтером (нет бюджета, проблемы с договором)
     * - APPROVED - одобрен, отправлен поставщику, ожидается поставка
     * - PARTIALLY_RECEIVED - частично получен товар (недопоставка)
     * - RECEIVED - полностью получен товар, заявка выполнена
     * - CANCELLED - отменен (может быть отменен на любом этапе)
     *
     * Переходы статусов контролируются бизнес-логикой:
     * - Заведующий может редактировать только DRAFT
     * - Директор может согласовать/отклонить только PENDING_DIRECTOR
     * - Бухгалтер может согласовать/отклонить только PENDING_ACCOUNTANT
     * - Переход в RECEIVED возможен только из APPROVED или PARTIALLY_RECEIVED
     *
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.DRAFT;

    /**
     * Поставщик, которому адресована заявка
     * Организация, у которой заказывается товар
     * Один поставщик может иметь множество заявок за разные периоды
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Supplier supplier;

    /**
     * Список позиций заявки (запрашиваемых товаров)
     * Каждая позиция содержит товар и требуемое количество
     *
     * Формируется заведующим складом на основе:
     * - Анализа текущих остатков на складе
     * - Прогноза спроса (планируемые заказы клиентов)
     * - Норм минимальных неснижаемых запасов
     * - Истории продаж и сезонности
     * - Срока годности товаров (особенно для скоропортящихся)
     *
     * Пример заявки:
     * Поставщик: "ООО Молочный комбинат"
     * Позиции:
     * - Молоко 3.2% 1л "Простоквашино" - 200 шт.
     * - Сметана 20% 500г "Простоквашино" - 100 шт.
     * - Творог 9% 200г "Домик в деревне" - 150 шт.
     *
     */
    @ToString.Exclude
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestedProduct> requestedProducts = new ArrayList<>();

    /**
     * Связь с фактической поставкой
     * После того как поставщик привезет товар, создается Delivery
     * и связывается с этой заявкой для контроля выполнения
     *
     * Используется для:
     * - Сопоставления заказанного и полученного (план vs факт)
     * - Контроля выполнения заявки
     * - Выявления расхождений (недопоставка, пересорт, брак)
     * - Формирования актов возврата при несоответствии
     * - Анализа надежности поставщика
     *
     * Пример сопоставления:
     * RequestedProduct: Молоко - 200 шт.
     * Supply: Молоко - 195 шт. → Недопоставка 5 шт.
     *
     * null - товар еще не поставлен или заявка отклонена/отменена
     */
    @ToString.Exclude
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создает новую заявку на поставку от поставщика
     * Автоматически устанавливает:
     * - Текущую дату как дату создания
     * - Начальный статус DRAFT (черновик)
     * - Пустой список позиций товаров (будут добавлены через addRequestedProduct)
     *
     * @param supplier поставщик, которому адресована заявка
     */
    public RequestForDelivery(Supplier supplier) {
        this.supplier = supplier;
        this.requestDate = LocalDate.now();
        this.status = RequestStatus.DRAFT;
        this.requestedProducts = new ArrayList<>();
    }

    // ========== МЕТОДЫ ==========

    /**
     * Добавляет позицию товара в заявку
     * Устанавливает двустороннюю связь между заявкой и позицией
     *
     * Используется при формировании заявки:
     * 1. Заведующий выбирает поставщика
     * 2. Создает заявку (RequestForDelivery)
     * 3. Для каждого товара создает RequestedProduct и добавляет через этот метод
     * 4. После добавления всех позиций отправляет на согласование
     *
     * @param requestedProduct позиция заявки (товар + требуемое количество)
     */
    public void addRequestedProduct(RequestedProduct requestedProduct) {
        this.requestedProducts.add(requestedProduct);
        requestedProduct.setRequest(this);
    }



}