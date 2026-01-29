package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_supplier")
@EqualsAndHashCode(of = "id")
//Поставщик
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(length = 100)
    private String contactInfo;
    @Column(length = 300)
    private String address;


    @Column(length = 12, unique = true)  // ИНН (10-12 цифр)
    private String inn;
    @Column(length = 20)  // КПП (9 цифр)
    private String kpp;
    @Column(length = 20)  // ОГРН/ОГРНИП (13-15 цифр)
    private String ogrn;
    @Column(length = 20)  // Расчетный счет (20 цифр)
    private String paymentAccount;
    @Column(length = 9)  // БИК (9 цифр)
    private String bik;
    @Column(length = 50)
    private String bank;

    @Column(length = 100, nullable = false)
    private String directorLastName; //Фамилия
    @Column(length = 100, nullable = false)
    private String directorFirstName; //Имя
    @Column(length = 100, nullable = false)  // Отчества может не быть, но в этом случае будем хранить пустую строку, а не null
    private String directorPatronymicName; //Отчество

    @Transient
    public String getFullName() {
        StringBuilder fullName = new StringBuilder(directorLastName);
        fullName.append(" ").append(directorFirstName);
        if (!directorPatronymicName.isEmpty()) {
            fullName.append(" ").append(directorPatronymicName);
        }
        return fullName.toString().trim();
    }

    @Transient
    public String getDirectorShortName() {
        StringBuilder shortName = new StringBuilder(directorLastName);
        shortName.append(" ").append(directorFirstName.charAt(0)).append(".");
        if (!directorPatronymicName.isEmpty()) {
            shortName.append(directorPatronymicName.charAt(0)).append(".");
        }
        return shortName.toString();
    }

    @ToString.Exclude
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Delivery> deliveries = new ArrayList<>();

    public Supplier(String name, String contactInfo, String address, String inn, String kpp, String ogrn, String paymentAccount, String bik, String bank, String directorLastName, String directorFirstName, String directorPatronymicName) {
        this.name = name;
        this.contactInfo = contactInfo;
        this.address = address;
        this.inn = inn;
        this.kpp = kpp;
        this.ogrn = ogrn;
        this.paymentAccount = paymentAccount;
        this.bik = bik;
        this.bank = bank;
        this.directorLastName = directorLastName;
        this.directorFirstName = directorFirstName;
        this.directorPatronymicName = directorPatronymicName;
    }
}
