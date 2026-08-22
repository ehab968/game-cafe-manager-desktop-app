package com.gamecafe.gamecafemanager.presentation.format;

import com.gamecafe.gamecafemanager.domain.service.SettingsProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Presentation-only formatting backed by the current cached settings.
 */
public final class ApplicationDisplayService {

    private final SettingsProvider settingsProvider;

    public ApplicationDisplayService(SettingsProvider settingsProvider) {
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
    }

    public String getCafeName() {
        return settingsProvider.getSettings().getCafeName();
    }

    public String formatMoney(BigDecimal amount) {
        return formatMoney(amount, settingsProvider.getSettings().getCurrencyDisplay());
    }

    public static String formatMoney(BigDecimal amount, String currencyDisplay) {
        Objects.requireNonNull(amount, "amount");
        String display = Objects.requireNonNull(currencyDisplay, "currencyDisplay").trim();
        String value = amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
        return display.isEmpty() ? value : display + " " + value;
    }
}
