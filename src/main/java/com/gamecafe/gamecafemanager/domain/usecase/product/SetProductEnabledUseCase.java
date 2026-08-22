package com.gamecafe.gamecafemanager.domain.usecase.product;

import com.gamecafe.gamecafemanager.domain.exception.ProductNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.repository.ProductRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.util.Objects;

public final class SetProductEnabledUseCase {

    private final ProductRepository repository;
    private final AuthorizationService authorization;

    public SetProductEnabledUseCase(
            ProductRepository repository,
            AuthorizationService authorization) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public void execute(long id, boolean enabled) {
        authorization.require(Permission.MANAGE_PRODUCTS);
        if (!repository.findById(id).isPresent()) {
            throw new ProductNotFoundException(id);
        }
        repository.setEnabled(id, enabled);
    }
}
