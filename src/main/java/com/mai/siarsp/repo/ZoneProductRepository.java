package com.mai.siarsp.repo;

import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.StorageZone;
import com.mai.siarsp.models.ZoneProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ZoneProductRepository extends JpaRepository<ZoneProduct, Long> {

    boolean existsByZoneAndProduct(StorageZone zone, Product product);

    Optional<ZoneProduct> findByZoneAndProduct(StorageZone zone, Product product);
}
