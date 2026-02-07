package com.mai.siarsp.repo;

import com.mai.siarsp.models.AcceptanceAct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcceptanceActRepository extends JpaRepository<AcceptanceAct, Long> {

    boolean existsByActNumber(String actNumber);

    Optional<AcceptanceAct> findByActNumber(String actNumber);
}
