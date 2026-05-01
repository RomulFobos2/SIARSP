package com.mai.siarsp.service.employee;

import com.mai.siarsp.models.AcceptanceAct;
import com.mai.siarsp.repo.AcceptanceActRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис чтения актов приёма-передачи для отображения в общем списке (с фильтром по статусу подписания).
 */
@Service
@Slf4j
public class AcceptanceActService {

    private final AcceptanceActRepository acceptanceActRepository;

    public AcceptanceActService(AcceptanceActRepository acceptanceActRepository) {
        this.acceptanceActRepository = acceptanceActRepository;
    }

    @Transactional(readOnly = true)
    public List<AcceptanceAct> getAllActs() {
        return acceptanceActRepository.findAllByOrderByActDateDesc();
    }

    @Transactional(readOnly = true)
    public List<AcceptanceAct> getActsByStatus(boolean signed) {
        return acceptanceActRepository.findBySignedOrderByActDateDesc(signed);
    }

    @Transactional(readOnly = true)
    public Optional<AcceptanceAct> getActById(Long id) {
        return acceptanceActRepository.findById(id);
    }
}
