package com.gamecafe.gamecafemanager.domain.usecase.product;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.model.ProductSale;
import com.gamecafe.gamecafemanager.domain.repository.ProductSaleRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import java.time.Clock;
import java.util.Objects;

/**
 * Records a product purchase without creating a station session.
 */
public final class SellProductUseCase {

    private final ProductSaleRepository repository;
    private final Clock clock;
    private final AuthorizationService authorization;

    public SellProductUseCase(
            ProductSaleRepository repository,
            Clock clock,
            AuthorizationService authorization) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public ProductSale execute(long productId, int quantity) {
        authorization.require(Permission.SELL_PRODUCTS);
        if (quantity <= 0) {
            throw ValidationException.forField(
                    "quantity", "Quantity must be greater than zero");
        }
        return repository.sell(productId, quantity, clock.instant());
    }
}
