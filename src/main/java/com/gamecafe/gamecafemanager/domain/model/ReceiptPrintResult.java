package com.gamecafe.gamecafemanager.domain.model;

import java.util.Objects;

public final class ReceiptPrintResult {

    private final ReceiptPrintStatus status;
    private final String message;

    public ReceiptPrintResult(ReceiptPrintStatus status, String message) {
        this.status = Objects.requireNonNull(status, "status");
        this.message = Objects.requireNonNull(message, "message");
    }

    public static ReceiptPrintResult of(ReceiptPrintStatus status, String message) {
        return new ReceiptPrintResult(status, message);
    }

    public ReceiptPrintStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isPrinted() {
        return status == ReceiptPrintStatus.PRINTED;
    }

    public boolean isFailure() {
        return status != ReceiptPrintStatus.PRINTED
                && status != ReceiptPrintStatus.AUTO_PRINT_DISABLED
                && status != ReceiptPrintStatus.CANCELLED;
    }
}
