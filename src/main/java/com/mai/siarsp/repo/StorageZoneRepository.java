package com.mai.siarsp.repo;

import com.mai.siarsp.models.Shelf;
import com.mai.siarsp.models.StorageZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StorageZoneRepository extends JpaRepository<StorageZone, Long> {

    boolean existsByShelfAndLabel(Shelf shelf, String label);

    Optional<StorageZone> findByShelfAndLabel(Shelf shelf, String label);

    List<StorageZone> findByShelf(Shelf shelf);
}
