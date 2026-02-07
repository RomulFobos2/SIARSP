package com.mai.siarsp.repo;

import com.mai.siarsp.models.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByInn(String inn);

    Optional<Client> findByInn(String inn);
}
