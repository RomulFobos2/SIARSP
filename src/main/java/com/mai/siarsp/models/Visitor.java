package com.mai.siarsp.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "t_visitor")
public class Visitor implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Column(length = 50)
    private String lastName; //Фамилия
    @NotNull
    @Column(length = 50)
    private String firstName; //Имя

    @Column(length = 50)
    private String patronymicName; //Отчество

    @NotNull
    @Column(length = 10)
    private String sex; //Пол

    @NotNull
    @DateTimeFormat(pattern = "dd.MM.yyyy")
    private LocalDate dateBirthday; //дата рождения

    @Column(unique = true)
    private String username; //Адрес почты, можно будет поменять на телефон

    @NotNull
    @Column(length = 200)
    private String password;

    @NotNull
    @Column(length = 20)
    private String mobileNumber;

    @Transient
    private String passwordConfirm;

    @DateTimeFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDate dateRegistration;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

//    @OneToMany(mappedBy = "visitor", fetch = FetchType.EAGER) //cascade = CascadeType.ALL, orphanRemoval = true
//    private Set<Order> orders = new HashSet<>();

    private boolean needChangePass = false;

    private boolean isActive = true;

    public Visitor(String lastName, String firstName, String patronymicName, String sex, LocalDate dateBirthday, String username, String password, String passwordConfirm, String mobileNumber) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.patronymicName = patronymicName;
        this.sex = sex;
        this.dateBirthday = dateBirthday;
        this.username = username;
        this.password = password;
        this.passwordConfirm = passwordConfirm;
        this.dateRegistration = LocalDate.now();
        this.mobileNumber = mobileNumber;
    }

    public Visitor(Long id, String lastName, String firstName, String patronymicName,
                   String sex, LocalDate dateBirthday, String username,
                   String mobileNumber, LocalDate dateRegistration, Role role, boolean needChangePass) {
        this.id = id;
        this.lastName = lastName;
        this.firstName = firstName;
        this.patronymicName = patronymicName;
        this.sex = sex;
        this.dateBirthday = dateBirthday;
        this.username = username;
        this.password = password;
        this.mobileNumber = mobileNumber;
        this.passwordConfirm = passwordConfirm;
        this.dateRegistration = dateRegistration;
        this.role = role;
        this.needChangePass = needChangePass;
        this.isActive = isActive;
    }

    @Transient
    private String fullName;
    public String getFullName() {
        return lastName + " " + firstName + " " + patronymicName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role.getName()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    @Override
    public String toString() {
        return "Visitor{" +
                "username='" + username + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", role=" + role +
                '}';
    }


}
