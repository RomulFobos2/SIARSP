package com.mai.siarsp.repo;

import com.mai.siarsp.models.AcceptanceAct;
import com.mai.siarsp.models.ClientOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcceptanceActRepository extends JpaRepository<AcceptanceAct, Long> {

    boolean existsByActNumber(String actNumber);

    Optional<AcceptanceAct> findByActNumber(String actNumber);

    Optional<AcceptanceAct> findByClientOrder(ClientOrder clientOrder);

    List<AcceptanceAct> findAllByOrderByActDateDesc();

    List<AcceptanceAct> findBySignedOrderByActDateDesc(boolean signed);
}
