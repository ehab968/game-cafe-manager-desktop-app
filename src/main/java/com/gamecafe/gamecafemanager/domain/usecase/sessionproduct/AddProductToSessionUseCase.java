package com.gamecafe.gamecafemanager.domain.usecase.sessionproduct;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.exception.SessionNotActiveException;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.model.SessionProduct;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
import com.gamecafe.gamecafemanager.domain.repository.SessionProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.util.Objects;

public final class AddProductToSessionUseCase {

    private final SessionRepository sessionRepository;
    private final SessionProductRepository sessionProductRepository;
    private final AuthorizationService authorization;

    public AddProductToSessionUseCase(
            SessionRepository sessionRepository,
            SessionProductRepository sessionProductRepository,
            AuthorizationService authorization) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository");
        this.sessionProductRepository = Objects.requireNonNull(
                sessionProductRepository, "sessionProductRepository");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public SessionProduct execute(long sessionId, long productId, int quantity) {
        authorization.require(Permission.ADD_SESSION_PRODUCTS);
        if (quantity <= 0) {
            throw ValidationException.forField("quantity", "Quantity must be greater than zero");
        }

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotActiveException(sessionId));
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new SessionNotActiveException(sessionId);
        }
        return sessionProductRepository.addToActiveSession(sessionId, productId, quantity);
    }
}
