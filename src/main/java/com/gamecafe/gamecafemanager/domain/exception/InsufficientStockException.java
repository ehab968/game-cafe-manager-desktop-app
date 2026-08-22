package com.gamecafe.gamecafemanager.domain.exception;

public final class InsufficientStockException extends RuntimeException {

    private final long productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(long productId, int requested, int available) {
        super("Product " + productId + " has " + available
                + " item(s) available; " + requested + " requested");
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public long getProductId() {
        return productId;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
