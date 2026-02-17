package com.mai.siarsp.controllers.employee.warehouseManager;

import com.mai.siarsp.dto.*;
import com.mai.siarsp.service.employee.warehouseManager.WarehouseApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-контроллер для операций управления складом.
 *
 * <p>Все эндпоинты доступны по пути {@code /employee/api/warehouse}.
 * Права доступа определены в конфигурации Spring Security —
 * все пути {@code /employee/**} требуют аутентификации сотрудника.</p>
 *
 * <p>Используется для AJAX-взаимодействия с фронтендом при работе
 * со страницами управления складом.</p>
 */
@RestController("warehouseApiController")
@RequestMapping("/employee/api/warehouse")
@Slf4j
public class WarehouseApiController {

    private final WarehouseApiService warehouseApiService;

    public WarehouseApiController(WarehouseApiService warehouseApiService) {
        this.warehouseApiService = warehouseApiService;
    }

    // ========== РАЗМЕЩЕНИЕ ==========

    /**
     * Автоматически размещает товар в оптимальную зону.
     *
     * <p>POST /employee/api/warehouse/place-product?productId=1&quantity=10</p>
     */
    @PostMapping("/place-product")
    public ResponseEntity<ApiResponse<PlacementInfo>> placeProduct(
            @RequestParam Long productId,
            @RequestParam int quantity) {
        try {
            PlacementInfo info = warehouseApiService.placeProductOptimal(productId, quantity);
            if (info.success()) {
                return ResponseEntity.ok(ApiResponse.ok(info, "Товар размещён успешно"));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error(info.reason()));
            }
        } catch (Exception e) {
            log.error("❌ REST /place-product: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Внутренняя ошибка сервера"));
        }
    }

    /**
     * Размещает товар в конкретную зону.
     *
     * <p>POST /employee/api/warehouse/place-product-in-zone?productId=1&zoneId=5&quantity=10</p>
     */
    @PostMapping("/place-product-in-zone")
    public ResponseEntity<ApiResponse<PlacementInfo>> placeProductInZone(
            @RequestParam Long productId,
            @RequestParam Long zoneId,
            @RequestParam int quantity) {
        try {
            PlacementInfo info = warehouseApiService.placeProductInSpecificZone(productId, zoneId, quantity);
            if (info.success()) {
                return ResponseEntity.ok(ApiResponse.ok(info, "Товар размещён в указанную зону"));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error(info.reason()));
            }
        } catch (Exception e) {
            log.error("❌ REST /place-product-in-zone: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Внутренняя ошибка сервера"));
        }
    }

    /**
     * Убирает товар из зоны хранения.
     *
     * <p>DELETE /employee/api/warehouse/remove-product?productId=1&zoneId=5&quantity=5</p>
     */
    @DeleteMapping("/remove-product")
    public ResponseEntity<ApiResponse<RemovalInfo>> removeProduct(
            @RequestParam Long productId,
            @RequestParam Long zoneId,
            @RequestParam int quantity) {
        try {
            RemovalInfo info = warehouseApiService.removeProduct(productId, zoneId, quantity);
            if (info.success()) {
                return ResponseEntity.ok(ApiResponse.ok(info, "Товар изъят из зоны"));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error(info.reason()));
            }
        } catch (Exception e) {
            log.error("❌ REST /remove-product: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Внутренняя ошибка сервера"));
        }
    }

    /**
     * Перемещает товар между зонами хранения.
     *
     * <p>POST /employee/api/warehouse/move-product?productId=1&fromZoneId=3&toZoneId=7&quantity=5</p>
     */
    @PostMapping("/move-product")
    public ResponseEntity<ApiResponse<MoveInfo>> moveProduct(
            @RequestParam Long productId,
            @RequestParam Long fromZoneId,
            @RequestParam Long toZoneId,
            @RequestParam int quantity) {
        try {
            MoveInfo info = warehouseApiService.moveProduct(productId, fromZoneId, toZoneId, quantity);
            if (info.success()) {
                return ResponseEntity.ok(ApiResponse.ok(info, "Товар перемещён"));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error(info.reason()));
            }
        } catch (Exception e) {
            log.error("❌ REST /move-product: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Внутренняя ошибка сервера"));
        }
    }

    // ========== ПРОВЕРКА И ИНФОРМАЦИЯ ==========

    /**
     * Проверяет возможность размещения товара (без изменения данных).
     *
     * <p>GET /employee/api/warehouse/check-placement?productId=1&quantity=10</p>
     */
    @GetMapping("/check-placement")
    public ResponseEntity<ApiResponse<PlacementCheck>> checkPlacement(
            @RequestParam Long productId,
            @RequestParam int quantity) {
        try {
            PlacementCheck check = warehouseApiService.checkPlacementPossibility(productId, quantity);
            return ResponseEntity.ok(ApiResponse.ok(check,
                    check.possible() ? "Размещение возможно" : check.reason()));
        } catch (Exception e) {
            log.error("❌ REST /check-placement: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Внутренняя ошибка сервера"));
        }
    }

    /**
     * Возвращает список мест хранения товара.
     *
     * <p>GET /employee/api/warehouse/product-locations?productId=1</p>
     */
    @GetMapping("/product-locations")
    public ResponseEntity<ApiResponse<List<ProductLocation>>> productLocations(
            @RequestParam Long productId) {
        try {
            List<ProductLocation> locations = warehouseApiService.findProductLocations(productId);
            return ResponseEntity.ok(ApiResponse.ok(locations, "Найдено мест хранения: " + locations.size()));
        } catch (Exception e) {
            log.error("❌ REST /product-locations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Внутренняя ошибка сервера"));
        }
    }

    /**
     * Возвращает детальную информацию о зоне хранения.
     *
     * <p>GET /employee/api/warehouse/zone-info?zoneId=5</p>
     */
    @GetMapping("/zone-info")
    public ResponseEntity<ApiResponse<ZoneInfo>> zoneInfo(@RequestParam Long zoneId) {
        try {
            return warehouseApiService.getZoneInfo(zoneId)
                    .map(info -> ResponseEntity.ok(ApiResponse.ok(info, "OK")))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("❌ REST /zone-info: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Внутренняя ошибка сервера"));
        }
    }

    /**
     * Возвращает статистику по складу.
     *
     * <p>GET /employee/api/warehouse/warehouse-stats?warehouseId=1</p>
     */
    @GetMapping("/warehouse-stats")
    public ResponseEntity<ApiResponse<WarehouseStatistics>> warehouseStats(@RequestParam Long warehouseId) {
        try {
            return warehouseApiService.getWarehouseStatistics(warehouseId)
                    .map(stats -> ResponseEntity.ok(ApiResponse.ok(stats, "OK")))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("❌ REST /warehouse-stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Внутренняя ошибка сервера"));
        }
    }
}
