package com.gamecafe.gamecafemanager.domain.usecase.session;

import com.gamecafe.gamecafemanager.domain.exception.SessionNotActiveException;
import com.gamecafe.gamecafemanager.domain.exception.SessionNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.CheckoutSummary;
import com.gamecafe.gamecafemanager.domain.model.GamingDiscount;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
import com.gamecafe.gamecafemanager.domain.repository.SessionProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.service.CheckoutService;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.time.Clock;
import java.util.Objects;

/**
 * Builds a checkout preview without changing session state.
 */
public final class PrepareCheckoutUseCase {

    private final SessionRepository sessionRepository;
    private final SessionProductRepository sessionProductRepository;
    private final CheckoutService checkoutService;
    private final Clock clock;
    private final AuthorizationService authorization;

    public PrepareCheckoutUseCase(
            SessionRepository sessionRepository,
            SessionProductRepository sessionProductRepository,
            CheckoutService checkoutService,
            Clock clock,
            AuthorizationService authorization) {
        this.sessionRepository = Objects.requireNonNull(
                sessionRepository, "sessionRepository");
        this.sessionProductRepository = Objects.requireNonNull(
                sessionProductRepository, "sessionProductRepository");
        this.checkoutService = Objects.requireNonNull(checkoutService, "checkoutService");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public CheckoutSummary execute(long sessionId) {
        return execute(sessionId, GamingDiscount.NONE);
    }

    public CheckoutSummary execute(long sessionId, GamingDiscount gamingDiscount) {
        authorization.require(Permission.CHECKOUT);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new SessionNotActiveException(sessionId);
        }
        return checkoutService.calculate(
                session,
                sessionProductRepository.findBySessionId(sessionId),
                clock.instant(),
                Objects.requireNonNull(gamingDiscount, "gamingDiscount"));
    }
}
