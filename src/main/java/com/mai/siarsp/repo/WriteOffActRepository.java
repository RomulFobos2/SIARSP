package com.mai.siarsp.repo;

import com.mai.siarsp.enumeration.WriteOffActStatus;
import com.mai.siarsp.models.WriteOffAct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WriteOffActRepository extends JpaRepository<WriteOffAct, Long> {

    boolean existsByActNumber(String actNumber);

    Optional<WriteOffAct> findByActNumber(String actNumber);

    List<WriteOffAct> findAllByOrderByActDateDesc();

    List<WriteOffAct> findByStatusOrderByActDateDesc(WriteOffActStatus status);

    List<WriteOffAct> findByResponsibleEmployeeIdOrderByActDateDesc(Long employeeId);
}
