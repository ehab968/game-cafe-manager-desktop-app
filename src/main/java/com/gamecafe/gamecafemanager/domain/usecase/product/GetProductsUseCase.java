package com.gamecafe.gamecafemanager.domain.usecase.product;

import com.gamecafe.gamecafemanager.domain.model.Product;
import com.gamecafe.gamecafemanager.domain.repository.ProductRepository;
import java.util.List;
import java.util.Objects;

public final class GetProductsUseCase {

    private final ProductRepository repository;

    public GetProductsUseCase(ProductRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<Product> execute() {
        return repository.findAll();
    }
}
