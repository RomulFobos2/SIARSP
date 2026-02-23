package com.mai.siarsp.repo;

import com.mai.siarsp.models.EquipmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentTypeRepository extends JpaRepository<EquipmentType, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<EquipmentType> findByName(String name);

    List<EquipmentType> findAllByOrderByNameAsc();
}
