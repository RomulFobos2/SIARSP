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

/**
 * Связка «партия-зона»: фактическое размещение конкретной партии (Supply) в зоне.
 * <p>
 * Раньше ссылалась на Product напрямую (одна запись на товар в зоне). Теперь ссылается на Supply,
 * благодаря чему в одной зоне могут лежать разные партии одного товара (с разными сроками годности),
 * а при отгрузке возможно списание FEFO.
 */

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_zoneProduct",
        uniqueConstraints = @UniqueConstraint(columnNames = {"zone_id", "supply_id"}))
@EqualsAndHashCode(of = "id")
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

    /**
     * Партия товара, размещённая в зоне. Nullable на уровне БД для совместимости с миграцией,
     * но всегда задаётся при создании новой ZoneProduct.
     */
    @ManyToOne
    @JoinColumn(name = "supply_id")
    private Supply supply;

    // ========== КОНСТРУКТОРЫ ==========

    public ZoneProduct(Supply supply, int quantity) {
        this.supply = supply;
        this.quantity = quantity;
    }

    // ========== МЕТОДЫ ==========

    /**
     * Товар, частью партии которого является эта ячейка хранения.
     * Совместимость со старым кодом, который обращался к ZoneProduct.getProduct().
     */
    @Transient
    public Product getProduct() {
        return supply != null ? supply.getProduct() : null;
    }

    public double getVolumePerUnit() {
        Product product = getProduct();
        if (product == null) return 0.0;
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

    public boolean canFitPhysically() {
        Product product = getProduct();
        if (product == null) return false;
        BoxOrientation bestOrientation = findBestOrientation(product, zone, quantity);

        if (bestOrientation != null) {
            this.orientation = bestOrientation;
            return true;
        }

        return false;
    }

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