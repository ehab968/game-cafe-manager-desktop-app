package com.gamecafe.gamecafemanager.domain.model;

import java.util.Objects;

public final class ReceiptPrintSettings {

    private final String selectedPrinterName;
    private final ReceiptPaperWidth paperWidth;
    private final boolean autoPrintAfterCheckout;

    public ReceiptPrintSettings(
            String selectedPrinterName,
            ReceiptPaperWidth paperWidth,
            boolean autoPrintAfterCheckout) {
        this.selectedPrinterName = normalizePrinterName(selectedPrinterName);
        this.paperWidth = Objects.requireNonNull(paperWidth, "paperWidth");
        this.autoPrintAfterCheckout = autoPrintAfterCheckout;
    }

    public static ReceiptPrintSettings defaults() {
        return new ReceiptPrintSettings(null, ReceiptPaperWidth.MM_80, false);
    }

    public String getSelectedPrinterName() {
        return selectedPrinterName;
    }

    public boolean hasSelectedPrinter() {
        return selectedPrinterName != null;
    }

    public ReceiptPaperWidth getPaperWidth() {
        return paperWidth;
    }

    public boolean isAutoPrintAfterCheckout() {
        return autoPrintAfterCheckout;
    }

    private String normalizePrinterName(String printerName) {
        if (printerName == null || printerName.trim().isEmpty()) {
            return null;
        }
        return printerName.trim();
    }
}
