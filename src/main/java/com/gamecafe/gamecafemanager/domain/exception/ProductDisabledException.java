package com.gamecafe.gamecafemanager.domain.exception;

public final class ProductDisabledException extends RuntimeException {

    public ProductDisabledException(long productId) {
        super("Product " + productId + " is disabled");
    }
}
