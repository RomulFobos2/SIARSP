package com.mai.siarsp.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

/**
 * Роль пользователя в системе. От нее зависят права на операции и доступ к разделам сервиса.
 */

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_role")
public class Role implements GrantedAuthority {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, unique = true)
    private String name;

    @NotNull
    @Column(nullable = false)
    private String description;

    // ========== КОНСТРУКТОРЫ ==========
    // Lombok @NoArgsConstructor создает конструктор без параметров

    // ========== МЕТОДЫ Spring Security ==========

    @Override
    public String getAuthority() {
        return getName();
    }

}