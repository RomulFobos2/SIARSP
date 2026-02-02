package com.mai.siarsp.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

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
    @Column(nullable = false)
    private String name;

    @NotNull
    @Column(nullable = false)
    private String description;

    // ========== КОНСТРУКТОРЫ ==========


    // ========== МЕТОДЫ ==========
    @Override
    public String getAuthority() {
        return getName();
    }

}
