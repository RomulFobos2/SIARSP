package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Клиент (конечный потребитель)
 *
 * Организация-получатель товара от ИП "Левчук". Согласно ТЗ, клиентами являются
 * детские дошкольные, общеобразовательные организации и лечебные учреждения города Байконур.
 * Хранит информацию о юридическом лице, адресах доставки и контактных данных.
 *
 * Связи:
 * - Один клиент может иметь множество заказов (ClientOrder)
 * - Один клиент может иметь множество актов приема-передачи (AcceptanceAct)
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_client")
@EqualsAndHashCode(of = "id")
public class Client {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Тип организации
     * Например: "Детский сад", "Школа", "Больница", "Поликлиника"
     * Согласно ТЗ, клиентами являются дошкольные, общеобразовательные
     * организации и лечебные учреждения
     */
    @Column(nullable = false, length = 300)
    private String organizationType;

    /**
     * Официальное наименование организации
     * Например: "МБДОУ Детский сад №5 'Солнышко'", "МБОУ СШ №12"
     */
    @Column(nullable = false, length = 300)
    private String organizationName;

    /**
     * ИНН организации (Идентификационный номер налогоплательщика)
     * 12 цифр для юридических лиц, 10 цифр для ИП
     */
    @Column(length = 12, unique = true)
    private String inn;

    /**
     * КПП (Код причины постановки на учет)
     */
    @Column(length = 20)
    private String kpp;

    /**
     * ОГРН (Основной государственный регистрационный номер)
     */
    @Column(length = 20)
    private String ogrn;

    /**
     * Юридический адрес организации
     * Официальный адрес по учредительным документам
     */
    @Column(length = 500)
    private String legalAddress;

    /**
     * Адрес доставки товара
     * Фактический адрес, куда осуществляется доставка
     * Может отличаться от юридического адреса
     * Используется водителем-экспедитором для маршрута
     */
    @Column(nullable = false, length = 500)
    private String deliveryAddress;

    /**
     * Широта адреса доставки (GPS)
     * Используется для автоматического заполнения маршрута доставки
     */
    @Column
    private Double deliveryLatitude;

    /**
     * Долгота адреса доставки (GPS)
     * Используется для автоматического заполнения маршрута доставки
     */
    @Column
    private Double deliveryLongitude;

    /**
     * Контактное лицо
     * ФИО и должность ответственного за приемку товара
     * Например: "Иванова Мария Петровна, заведующая складом"
     */
    @Column(length = 200)
    private String contactPerson;

    /**
     * Контактный номер телефона
     * Для связи при доставке и согласовании заказов
     */
    @Column(length = 20)
    private String phoneNumber;

    /**
     * Адрес электронной почты
     * Для отправки документов (договоров, накладных, актов)
     */
    @Column(length = 100)
    private String email;

    /**
     * Список заказов клиента
     * История всех заказов, размещенных данным клиентом
     */
    @ToString.Exclude
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClientOrder> orders = new ArrayList<>();

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создает нового клиента с минимальным набором полей
     *
     * @param organizationType тип организации (детский сад, школа, больница)
     * @param organizationName официальное название организации
     * @param deliveryAddress адрес доставки товара
     * @param contactPerson ФИО и должность контактного лица
     */
    public Client(String organizationType, String organizationName,
                  String deliveryAddress, String contactPerson) {
        this.organizationType = organizationType;
        this.organizationName = organizationName;
        this.deliveryAddress = deliveryAddress;
        this.contactPerson = contactPerson;
    }

    // ========== МЕТОДЫ ==========

    /**
     * Возвращает полное отображаемое имя клиента
     * Комбинирует название организации и её тип для удобства
     *
     * @return строка вида "МБДОУ Детский сад №5 (Детский сад)"
     */
    @Transient
    public String getDisplayName() {
        return organizationName + " (" + organizationType + ")";
    }
}