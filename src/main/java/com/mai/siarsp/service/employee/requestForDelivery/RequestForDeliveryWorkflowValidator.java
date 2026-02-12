package com.mai.siarsp.service.employee.requestForDelivery;

import com.mai.siarsp.enumeration.RequestStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class RequestForDeliveryWorkflowValidator {

    private enum Actor {
        WAREHOUSE_MANAGER,
        MANAGER,
        ACCOUNTER
    }

    private record TransitionRule(Set<RequestStatus> allowedStatuses, Actor actor) {}

    private static final Map<RequestStatus, TransitionRule> RULES = Map.of(
            RequestStatus.DRAFT, new TransitionRule(Set.of(RequestStatus.PENDING_DIRECTOR, RequestStatus.CANCELLED), Actor.WAREHOUSE_MANAGER),
            RequestStatus.PENDING_DIRECTOR, new TransitionRule(Set.of(RequestStatus.PENDING_ACCOUNTANT, RequestStatus.REJECTED_BY_DIRECTOR), Actor.MANAGER),
            RequestStatus.REJECTED_BY_DIRECTOR, new TransitionRule(Set.of(RequestStatus.PENDING_DIRECTOR, RequestStatus.CANCELLED), Actor.WAREHOUSE_MANAGER),
            RequestStatus.PENDING_ACCOUNTANT, new TransitionRule(Set.of(RequestStatus.APPROVED, RequestStatus.REJECTED_BY_ACCOUNTANT), Actor.ACCOUNTER),
            RequestStatus.REJECTED_BY_ACCOUNTANT, new TransitionRule(Set.of(RequestStatus.PENDING_DIRECTOR, RequestStatus.CANCELLED), Actor.WAREHOUSE_MANAGER),
            RequestStatus.APPROVED, new TransitionRule(Set.of(RequestStatus.PARTIALLY_RECEIVED, RequestStatus.RECEIVED), Actor.WAREHOUSE_MANAGER),
            RequestStatus.PARTIALLY_RECEIVED, new TransitionRule(Set.of(RequestStatus.RECEIVED), Actor.WAREHOUSE_MANAGER)
    );

    public boolean canTransition(RequestStatus from, RequestStatus to, String roleName) {
        TransitionRule rule = RULES.get(from);
        if (rule == null) {
            log.warn("Не найдено правило перехода статуса для {}", from);
            return false;
        }

        if (!rule.allowedStatuses().contains(to)) {
            log.warn("Недопустимый переход статуса заявки: {} -> {}", from, to);
            return false;
        }

        Actor actor = mapRole(roleName);
        if (actor == null || actor != rule.actor()) {
            log.warn("Недопустимая роль '{}' для перехода {} -> {}", roleName, from, to);
            return false;
        }

        return true;
    }

    private Actor mapRole(String roleName) {
        return switch (roleName) {
            case "ROLE_EMPLOYEE_WAREHOUSE_MANAGER" -> Actor.WAREHOUSE_MANAGER;
            case "ROLE_EMPLOYEE_MANAGER" -> Actor.MANAGER;
            case "ROLE_EMPLOYEE_ACCOUNTER" -> Actor.ACCOUNTER;
            default -> null;
        };
    }
}
