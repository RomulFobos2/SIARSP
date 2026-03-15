package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Карточка клиента (школа, сад, медучреждение и т.д.). Хранит реквизиты и контактные данные, которые используются в заказах и документах.
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

    @Column(nullable = false, length = 300)
    private String organizationType;

    @Column(nullable = false, length = 300)
    private String organizationName;

    @Column(length = 12, unique = true)
    private String inn;

    @Column(length = 20)
    private String kpp;

    @Column(length = 20)
    private String ogrn;

    @Column(length = 500)
    private String legalAddress;

    @Column(nullable = false, length = 500)
    private String deliveryAddress;

    @Column
    private Double deliveryLatitude;

    @Column
    private Double deliveryLongitude;

    @Column(length = 500)
    private String deliveryLocationName;

    @Column(length = 200)
    private String contactPerson;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 100)
    private String email;

    @ToString.Exclude
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClientOrder> orders = new ArrayList<>();

    // ========== КОНСТРУКТОРЫ ==========

    public Client(String organizationType, String organizationName,
                  String deliveryAddress, String contactPerson) {
        this.organizationType = organizationType;
        this.organizationName = organizationName;
        this.deliveryAddress = deliveryAddress;
        this.contactPerson = contactPerson;
    }

    // ========== МЕТОДЫ ==========

    @Transient
    public String getDisplayName() {
        return organizationName + " (" + organizationType + ")";
    }
}