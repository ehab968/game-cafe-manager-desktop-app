package com.gamecafe.gamecafemanager.domain.usecase.sessionproduct;

import com.gamecafe.gamecafemanager.domain.model.SessionProduct;
import com.gamecafe.gamecafemanager.domain.repository.SessionProductRepository;
import java.util.List;
import java.util.Objects;

public final class GetSessionProductsUseCase {

    private final SessionProductRepository repository;

    public GetSessionProductsUseCase(SessionProductRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<SessionProduct> execute(long sessionId) {
        return repository.findBySessionId(sessionId);
    }
}
