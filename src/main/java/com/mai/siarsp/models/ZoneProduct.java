package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.BoxOrientation;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_zoneProduct")
@EqualsAndHashCode(of = "id")
// --- 4. Товар на полке
public class ZoneProduct {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoxOrientation orientation = BoxOrientation.STANDARD;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private StorageZone zone;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Product product;

    // ========== КОНСТРУКТОРЫ ==========
    public ZoneProduct(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    // ========== МЕТОДЫ ==========
    public double getVolumePerUnit() {
        Double length = product.getPackageLength();
        Double width = product.getPackageWidth();
        Double height = product.getPackageHeight();

        if (length == null || width == null || height == null) {
            return 0.0;
        }

        return (length / 100.0) * (width / 100.0) * (height / 100.0);
    }

    public double getTotalVolume() {
        return getVolumePerUnit() * quantity;
    }

    // TODO: Перенести в ZoneProductService - сложная бизнес-логика размещения
    public boolean canFitPhysically() {
        BoxOrientation bestOrientation = findBestOrientation(product, zone, quantity);

        if (bestOrientation != null) {
            this.orientation = bestOrientation;
            return true;
        }

        return false;
    }

    // TODO: Перенести в ZoneProductService - алгоритм оптимального размещения
    public BoxOrientation findBestOrientation(Product product, StorageZone zone, int quantity) {
        Double boxL = product.getPackageLength();
        Double boxW = product.getPackageWidth();
        Double boxH = product.getPackageHeight();

        // Проверка на null габаритов
        if (boxL == null || boxW == null || boxH == null) {
            return null;
        }

        return Arrays.stream(BoxOrientation.values())
                .map(orientation -> {
                    double[] dims = applyOrientation(boxL, boxW, boxH, orientation);
                    int fitL = (int) (zone.getLength() / dims[0]);
                    int fitW = (int) (zone.getWidth() / dims[1]);
                    int fitH = (int) (zone.getHeight() / dims[2]);
                    int maxQty = fitL * fitW * fitH;
                    return Map.entry(orientation, maxQty);
                })
                .filter(e -> e.getValue() >= quantity)
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private double[] applyOrientation(double l, double w, double h, BoxOrientation orientation) {
        return switch (orientation) {
            case STANDARD -> new double[]{l, w, h};
            case ROTATED_90 -> new double[]{w, l, h};
            case LAY_ON_SIDE -> new double[]{l, h, w};
            case ROTATE_AND_LAY -> new double[]{w, h, l};
        };
    }
}
