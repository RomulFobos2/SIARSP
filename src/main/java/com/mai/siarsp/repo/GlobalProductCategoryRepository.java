package com.mai.siarsp.repo;

import com.mai.siarsp.models.GlobalProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GlobalProductCategoryRepository extends JpaRepository<GlobalProductCategory, Long> {

    boolean existsByName(String name);

    Optional<GlobalProductCategory> findByName(String name);
}
