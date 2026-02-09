package com.mai.siarsp.repo;

import com.mai.siarsp.models.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumberAndIdNot(String registrationNumber, Long id);

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);
}
