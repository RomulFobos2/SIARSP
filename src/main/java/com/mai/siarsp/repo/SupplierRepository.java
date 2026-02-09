package com.mai.siarsp.repo;

import com.mai.siarsp.models.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsByInn(String inn);

    boolean existsByInnAndIdNot(String inn, Long id);

    Optional<Supplier> findByInn(String inn);
}
