package com.mai.siarsp.repo;

import com.mai.siarsp.models.TTN;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TTNRepository extends JpaRepository<TTN, Long> {

    boolean existsByTtnNumber(String ttnNumber);

    Optional<TTN> findByTtnNumber(String ttnNumber);
}
