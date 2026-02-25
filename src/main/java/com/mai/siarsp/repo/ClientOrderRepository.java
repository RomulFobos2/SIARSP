package com.mai.siarsp.repo;

import com.mai.siarsp.enumeration.ClientOrderStatus;
import com.mai.siarsp.models.ClientOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientOrderRepository extends JpaRepository<ClientOrder, Long> {

    boolean existsByOrderNumber(String orderNumber);

    Optional<ClientOrder> findByOrderNumber(String orderNumber);

    List<ClientOrder> findAllByOrderByOrderDateDesc();

    List<ClientOrder> findByStatusOrderByOrderDateDesc(ClientOrderStatus status);

    List<ClientOrder> findByClientIdOrderByOrderDateDesc(Long clientId);

    @Query("SELECT co FROM ClientOrder co WHERE co.status IN :statuses ORDER BY co.orderDate DESC")
    List<ClientOrder> findByStatusInOrderByOrderDateDesc(@Param("statuses") List<ClientOrderStatus> statuses);

    @Query("SELECT co FROM ClientOrder co " +
           "LEFT JOIN FETCH co.orderedProducts op " +
           "LEFT JOIN FETCH op.product " +
           "LEFT JOIN FETCH co.client " +
           "LEFT JOIN FETCH co.responsibleEmployee " +
           "WHERE co.id = :id")
    Optional<ClientOrder> findByIdWithDetails(@Param("id") Long id);
}
