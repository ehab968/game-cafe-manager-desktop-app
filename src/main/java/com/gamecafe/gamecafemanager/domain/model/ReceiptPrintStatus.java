package com.gamecafe.gamecafemanager.domain.model;

public enum ReceiptPrintStatus {
    PRINTED,
    AUTO_PRINT_DISABLED,
    NO_PRINTERS_AVAILABLE,
    PRINTER_NOT_CONFIGURED,
    PRINTER_NOT_FOUND,
    PRINTER_UNAVAILABLE,
    CANCELLED,
    FAILED
}
