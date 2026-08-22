package com.gamecafe.gamecafemanager.domain.usecase.product;

import com.gamecafe.gamecafemanager.domain.exception.ProductNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.repository.ProductRepository;
import com.gamecafe.gamecafemanager.domain.service.ProductValidator;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.util.Objects;

public final class UpdateProductStockUseCase {

    private final ProductRepository repository;
    private final ProductValidator validator;
    private final AuthorizationService authorization;

    public UpdateProductStockUseCase(
            ProductRepository repository,
            ProductValidator validator,
            AuthorizationService authorization) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public void execute(long id, int stockQuantity) {
        authorization.require(Permission.MANAGE_PRODUCTS);
        validator.validateStock(stockQuantity);
        if (!repository.findById(id).isPresent()) {
            throw new ProductNotFoundException(id);
        }
        repository.updateStock(id, stockQuantity);
    }
}
