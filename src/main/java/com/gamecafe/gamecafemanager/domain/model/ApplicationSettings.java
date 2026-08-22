package com.gamecafe.gamecafemanager.domain.model;

import java.util.Objects;

public final class ApplicationSettings {

    private static final String DEFAULT_CAFE_NAME = "Game Cafe";
    private static final String DEFAULT_CURRENCY_DISPLAY = "EGP";
    private static final String DEFAULT_INVOICE_FOOTER = "Thank you for visiting!";

    private final String cafeName;
    private final String currencyDisplay;
    private final String invoiceFooter;
    private final Integer minimumSessionMinutes;
    private final Integer billingRoundingMinutes;

    public ApplicationSettings(
            String cafeName,
            String currencyDisplay,
            String invoiceFooter,
            Integer minimumSessionMinutes,
            Integer billingRoundingMinutes) {
        this.cafeName = Objects.requireNonNull(cafeName, "cafeName");
        this.currencyDisplay = Objects.requireNonNull(currencyDisplay, "currencyDisplay");
        this.invoiceFooter = Objects.requireNonNull(invoiceFooter, "invoiceFooter");
        this.minimumSessionMinutes = minimumSessionMinutes;
        this.billingRoundingMinutes = billingRoundingMinutes;
    }

    public static ApplicationSettings defaults() {
        return new ApplicationSettings(
                DEFAULT_CAFE_NAME,
                DEFAULT_CURRENCY_DISPLAY,
                DEFAULT_INVOICE_FOOTER,
                null,
                null);
    }

    public String getCafeName() {
        return cafeName;
    }

    public String getCurrencyDisplay() {
        return currencyDisplay;
    }

    public String getInvoiceFooter() {
        return invoiceFooter;
    }

    public Integer getMinimumSessionMinutes() {
        return minimumSessionMinutes;
    }

    public Integer getBillingRoundingMinutes() {
        return billingRoundingMinutes;
    }
}
