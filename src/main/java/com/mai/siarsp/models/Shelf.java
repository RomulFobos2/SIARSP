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
@Table(name = "t_shelf")
@EqualsAndHashCode(of = "id")
// --- 2. Сущность стеллажа
public class Shelf {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Warehouse warehouse;

    @ToString.Exclude
    @OneToMany(mappedBy = "shelf", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StorageZone> storageZones = new ArrayList<>();

    // ========== КОНСТРУКТОРЫ ==========
    public Shelf(String code, Warehouse warehouse) {
        this.code = code;
        this.warehouse = warehouse;
    }

    // ========== МЕТОДЫ ==========
}
