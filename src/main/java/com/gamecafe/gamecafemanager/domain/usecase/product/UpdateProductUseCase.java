package com.gamecafe.gamecafemanager.domain.usecase.product;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.exception.ProductNotFoundException;
import com.gamecafe.gamecafemanager.domain.model.Product;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.repository.ProductRepository;
import com.gamecafe.gamecafemanager.domain.service.ProductValidator;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.math.BigDecimal;
import java.util.Objects;

public final class UpdateProductUseCase {

    private final ProductRepository repository;
    private final ProductValidator validator;
    private final AuthorizationService authorization;

    public UpdateProductUseCase(
            ProductRepository repository,
            ProductValidator validator,
            AuthorizationService authorization) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public Product execute(long id, String name, BigDecimal currentPrice, int stockQuantity) {
        authorization.require(Permission.MANAGE_PRODUCTS);
        validator.validate(name, currentPrice, stockQuantity);
        Product existing = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        String normalizedName = validator.normalizeName(name);
        if (repository.existsByName(normalizedName, id)) {
            throw ValidationException.forField("name", "A product with this name already exists");
        }
        return repository.update(new Product(
                id,
                normalizedName,
                validator.normalizeCurrentPrice(currentPrice),
                stockQuantity,
                existing.isEnabled()));
    }
}
