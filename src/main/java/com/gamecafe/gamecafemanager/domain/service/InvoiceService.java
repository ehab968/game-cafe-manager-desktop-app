package com.gamecafe.gamecafemanager.domain.service;

import com.gamecafe.gamecafemanager.domain.exception.SessionNotCompletedException;
import com.gamecafe.gamecafemanager.domain.model.Invoice;
import com.gamecafe.gamecafemanager.domain.model.InvoiceItem;
import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionProduct;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Generates an output-neutral invoice from completed checkout snapshots.
 */
public final class InvoiceService {

    private final SettingsProvider settingsProvider;

    public InvoiceService(SettingsProvider settingsProvider) {
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
    }

    public Invoice generate(Session session, List<SessionProduct> purchasedProducts) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(purchasedProducts, "purchasedProducts");
        if (session.getId() == null) {
            throw new IllegalArgumentException("Persisted session id is required for invoice");
        }
        if (session.getStatus() != SessionStatus.COMPLETED || session.getEndTime() == null) {
            throw new SessionNotCompletedException(session.getId());
        }

        List<InvoiceItem> items = new ArrayList<>();
        for (SessionProduct product : purchasedProducts) {
            if (product.getSessionId() != session.getId()) {
                throw new IllegalArgumentException("Invoice item belongs to another session");
            }
            items.add(new InvoiceItem(
                    product.getProductId(),
                    product.getProductNameSnapshot(),
                    product.getUnitPriceSnapshot(),
                    product.getQuantity(),
                    product.getLineTotal()));
        }

        ApplicationSettings settings = settingsProvider.getSettings();
        return new Invoice(
                settings.getCafeName(),
                settings.getCurrencyDisplay(),
                settings.getInvoiceFooter(),
                String.format(Locale.ROOT, "INV-%06d", session.getId()),
                session.getId(),
                session.getStationNameSnapshot(),
                session.getStationTypeSnapshot(),
                session.getMode(),
                session.getHourlyRateSnapshot(),
                session.getStartTime(),
                session.getEndTime(),
                Duration.between(session.getStartTime(), session.getEndTime()),
                session.getPlayCost(),
                items,
                session.getProductsCost(),
                session.getFinalTotal());
    }
}
