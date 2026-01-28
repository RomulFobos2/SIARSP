package com.mai.siarsp.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_employee")
public class Employee implements UserDetails {
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
    @NotNull
    private String patronymicName; //Отчество

    @Column(unique = true, length = 50)
    @NotNull
    private String username;

    @NotNull
    @Column(length = 200)
    private String password;


    private boolean needChangePass = true;

    private boolean isActive = true;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @DateTimeFormat(pattern = "dd.MM.yyyy")
    private LocalDate dateOfRegistration;

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

    public String getFullName() {
        return lastName + " " + firstName + " " + patronymicName;
    }

    public Employee(String lastName, String firstName, String patronymicName, String username, String password) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.patronymicName = patronymicName;
        this.username = username;
        this.password = password;
        this.dateOfRegistration = LocalDate.now();
    }

    @Override
    public String toString() {
        return "Employee{" +
                "username='" + username + '\'' +
                ", role=" + role.getName() +
                '}';
    }
}