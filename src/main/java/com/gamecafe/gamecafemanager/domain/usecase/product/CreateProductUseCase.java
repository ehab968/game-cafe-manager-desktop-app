package com.gamecafe.gamecafemanager.domain.usecase.product;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.Product;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.repository.ProductRepository;
import com.gamecafe.gamecafemanager.domain.service.ProductValidator;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.math.BigDecimal;
import java.util.Objects;

public final class CreateProductUseCase {

    private final ProductRepository repository;
    private final ProductValidator validator;
    private final AuthorizationService authorization;

    public CreateProductUseCase(
            ProductRepository repository,
            ProductValidator validator,
            AuthorizationService authorization) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public Product execute(String name, BigDecimal currentPrice, int stockQuantity) {
        authorization.require(Permission.MANAGE_PRODUCTS);
        validator.validate(name, currentPrice, stockQuantity);
        String normalizedName = validator.normalizeName(name);
        if (repository.existsByName(normalizedName, null)) {
            throw ValidationException.forField("name", "A product with this name already exists");
        }
        return repository.create(new Product(
                null,
                normalizedName,
                validator.normalizeCurrentPrice(currentPrice),
                stockQuantity,
                true));
    }
}
