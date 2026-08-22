package com.gamecafe.gamecafemanager.domain.usecase.session;

import com.gamecafe.gamecafemanager.domain.exception.DuplicateCheckoutException;
import com.gamecafe.gamecafemanager.domain.exception.SessionNotActiveException;
import com.gamecafe.gamecafemanager.domain.exception.SessionNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.CheckoutSummary;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
import com.gamecafe.gamecafemanager.domain.repository.SessionProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.service.CheckoutService;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class FinishSessionUseCase {

    private final SessionRepository repository;
    private final SessionProductRepository sessionProductRepository;
    private final CheckoutService checkoutService;
    private final Clock clock;
    private final AuthorizationService authorization;

    public FinishSessionUseCase(
            SessionRepository repository,
            SessionProductRepository sessionProductRepository,
            CheckoutService checkoutService,
            Clock clock,
            AuthorizationService authorization) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.sessionProductRepository = Objects.requireNonNull(
                sessionProductRepository, "sessionProductRepository");
        this.checkoutService = Objects.requireNonNull(checkoutService, "checkoutService");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public Session execute(long sessionId) {
        return execute(sessionId, clock.instant());
    }

    public Session execute(long sessionId, Instant endTime) {
        authorization.require(Permission.CHECKOUT);
        Session activeSession = repository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        if (activeSession.getStatus() == SessionStatus.COMPLETED) {
            throw new DuplicateCheckoutException(sessionId);
        }
        if (activeSession.getStatus() != SessionStatus.ACTIVE) {
            throw new SessionNotActiveException(sessionId);
        }

        CheckoutSummary checkout = checkoutService.calculate(
                activeSession,
                sessionProductRepository.findBySessionId(sessionId),
                Objects.requireNonNull(endTime, "endTime"));

        return repository.finish(new Session(
                activeSession.getId(),
                activeSession.getStationId(),
                activeSession.getStationNameSnapshot(),
                activeSession.getStationTypeSnapshot(),
                activeSession.getStartTime(),
                endTime,
                SessionStatus.COMPLETED,
                activeSession.getHourlyRateSnapshot(),
                checkout.getGamingCost(),
                checkout.getProductsTotal(),
                checkout.getFinalTotal()));
    }
}
