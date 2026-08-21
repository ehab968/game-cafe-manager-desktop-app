package com.gamecafe.gamecafemanager.domain.usecase.session;

import com.gamecafe.gamecafemanager.domain.exception.SessionNotActiveException;
import com.gamecafe.gamecafemanager.domain.exception.SessionNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.domain.service.pricing.PricingResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class FinishSessionUseCase {

    private final SessionRepository repository;
    private final PricingService pricingService;
    private final Clock clock;

    public FinishSessionUseCase(
            SessionRepository repository,
            PricingService pricingService,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.pricingService = Objects.requireNonNull(pricingService, "pricingService");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Session execute(long sessionId) {
        Session activeSession = repository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        if (activeSession.getStatus() != SessionStatus.ACTIVE) {
            throw new SessionNotActiveException(sessionId);
        }

        Instant endTime = clock.instant();
        PricingResult pricingResult = pricingService.calculate(
                activeSession.getStartTime(),
                endTime,
                activeSession.getHourlyRateSnapshot());
        BigDecimal playCost = pricingResult.getGamingPrice();
        BigDecimal productsCost = activeSession.getProductsCost()
                .setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal finalTotal = playCost.add(productsCost).setScale(2, RoundingMode.UNNECESSARY);

        return repository.finish(new Session(
                activeSession.getId(),
                activeSession.getStationId(),
                activeSession.getStationNameSnapshot(),
                activeSession.getStartTime(),
                endTime,
                SessionStatus.COMPLETED,
                activeSession.getHourlyRateSnapshot(),
                playCost,
                productsCost,
                finalTotal));
    }
}
