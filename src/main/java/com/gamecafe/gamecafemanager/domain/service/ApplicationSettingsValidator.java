package com.gamecafe.gamecafemanager.domain.service;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintSettings;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ApplicationSettingsValidator {

    private static final int MAXIMUM_CAFE_NAME_LENGTH = 100;
    private static final int MAXIMUM_CURRENCY_DISPLAY_LENGTH = 12;
    private static final int MAXIMUM_INVOICE_FOOTER_LENGTH = 500;
    private static final int MAXIMUM_PRINTER_NAME_LENGTH = 255;
    private static final int MAXIMUM_DURATION_MINUTES = 1_440;

    public ApplicationSettings validateAndNormalize(ApplicationSettings settings) {
        Objects.requireNonNull(settings, "settings");
        String cafeName = settings.getCafeName().trim();
        String currencyDisplay = settings.getCurrencyDisplay().trim();
        String invoiceFooter = settings.getInvoiceFooter().trim();
        ReceiptPrintSettings printSettings = settings.getReceiptPrintSettings();
        String printerName = printSettings.getSelectedPrinterName();
        Map<String, String> errors = new LinkedHashMap<>();

        if (cafeName.isEmpty()) {
            errors.put("cafeName", "Cafe name is required");
        } else if (cafeName.length() > MAXIMUM_CAFE_NAME_LENGTH) {
            errors.put("cafeName", "Cafe name must not exceed 100 characters");
        }
        if (currencyDisplay.isEmpty()) {
            errors.put("currencyDisplay", "Currency display is required");
        } else if (currencyDisplay.length() > MAXIMUM_CURRENCY_DISPLAY_LENGTH) {
            errors.put("currencyDisplay", "Currency display must not exceed 12 characters");
        }
        if (invoiceFooter.length() > MAXIMUM_INVOICE_FOOTER_LENGTH) {
            errors.put("invoiceFooter", "Invoice footer must not exceed 500 characters");
        }
        validateOptionalMinutes(
                "minimumSessionMinutes",
                "Minimum session duration",
                settings.getMinimumSessionMinutes(),
                errors);
        validateOptionalMinutes(
                "billingRoundingMinutes",
                "Billing rounding interval",
                settings.getBillingRoundingMinutes(),
                errors);
        if (printerName != null && printerName.length() > MAXIMUM_PRINTER_NAME_LENGTH) {
            errors.put("receiptPrinter", "Printer name must not exceed 255 characters");
        }
        if (printSettings.isAutoPrintAfterCheckout() && printerName == null) {
            errors.put(
                    "autoPrintAfterCheckout",
                    "Select a receipt printer before enabling automatic printing");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new ApplicationSettings(
                cafeName,
                currencyDisplay,
                invoiceFooter,
                settings.getMinimumSessionMinutes(),
                settings.getBillingRoundingMinutes(),
                new ReceiptPrintSettings(
                        printerName,
                        printSettings.getPaperWidth(),
                        printSettings.isAutoPrintAfterCheckout()));
    }

    private void validateOptionalMinutes(
            String field,
            String label,
            Integer value,
            Map<String, String> errors) {
        if (value != null && (value < 1 || value > MAXIMUM_DURATION_MINUTES)) {
            errors.put(field, label + " must be between 1 and 1440 minutes");
        }
    }
}
