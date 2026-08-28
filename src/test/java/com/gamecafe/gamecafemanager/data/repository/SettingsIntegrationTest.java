package com.gamecafe.gamecafemanager.data.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.data.sqlite.SQLiteDatabase;
import com.gamecafe.gamecafemanager.domain.exception.AuthorizationException;
import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.model.Role;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPaperWidth;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintSettings;
import com.gamecafe.gamecafemanager.domain.repository.SettingsRepository;
import com.gamecafe.gamecafemanager.domain.service.ApplicationSettingsService;
import com.gamecafe.gamecafemanager.domain.service.ApplicationSettingsValidator;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.domain.service.pricing.GamingPricePolicy;
import com.gamecafe.gamecafemanager.domain.service.pricing.MonetaryRoundingPolicy;
import com.gamecafe.gamecafemanager.domain.service.pricing.PricingResult;
import com.gamecafe.gamecafemanager.domain.service.pricing.SettingsBillableDurationPolicy;
import com.gamecafe.gamecafemanager.domain.usecase.settings.GetSettingsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.settings.UpdateSettingsUseCase;
import com.gamecafe.gamecafemanager.presentation.format.ApplicationDisplayService;
import com.gamecafe.gamecafemanager.support.AuthenticationTestSupport;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsIntegrationTest {

    private static final Instant START_TIME = Instant.parse("2026-08-22T12:00:00Z");

    @TempDir
    Path temporaryDirectory;

    private SQLiteDatabase database;
    private SettingsRepository repository;
    private ApplicationSettingsService settingsService;
    private AuthorizationService authorization;
    private GetSettingsUseCase getSettings;
    private UpdateSettingsUseCase updateSettings;

    @BeforeEach
    void setUp() {
        database = new SQLiteDatabase(temporaryDirectory.resolve("settings.db"));
        database.initialize();
        repository = new SQLiteSettingsRepository(database);
        settingsService = new ApplicationSettingsService(repository);
        authorization = AuthenticationTestSupport.authenticatedAdmin(database);
        getSettings = new GetSettingsUseCase(settingsService, authorization);
        updateSettings = new UpdateSettingsUseCase(
                settingsService,
                new ApplicationSettingsValidator(),
                authorization);
    }

    @Test
    void seedsDefaultsNormalizesUpdatesAndSurvivesServiceReconstruction() {
        ApplicationSettings defaults = getSettings.execute();
        assertEquals("Game Cafe", defaults.getCafeName());
        assertEquals("EGP", defaults.getCurrencyDisplay());
        assertEquals("Thank you for visiting!", defaults.getInvoiceFooter());
        assertNull(defaults.getMinimumSessionMinutes());
        assertNull(defaults.getBillingRoundingMinutes());
        assertNull(defaults.getReceiptPrintSettings().getSelectedPrinterName());
        assertEquals(
                ReceiptPaperWidth.MM_80,
                defaults.getReceiptPrintSettings().getPaperWidth());
        assertEquals(false, defaults.getReceiptPrintSettings().isAutoPrintAfterCheckout());

        ApplicationSettings saved = updateSettings.execute(new ApplicationSettings(
                "  Pixel Hub  ",
                " USD ",
                "  Thanks for playing!  ",
                30,
                15,
                new ReceiptPrintSettings(
                        " Microsoft Print to PDF ",
                        ReceiptPaperWidth.MM_58,
                        true)));
        assertEquals("Pixel Hub", saved.getCafeName());
        assertEquals("USD", saved.getCurrencyDisplay());
        assertEquals("Thanks for playing!", saved.getInvoiceFooter());

        ApplicationSettings reconstructed =
                new ApplicationSettingsService(new SQLiteSettingsRepository(database))
                        .getSettings();
        assertEquals("Pixel Hub", reconstructed.getCafeName());
        assertEquals("USD", reconstructed.getCurrencyDisplay());
        assertEquals("Thanks for playing!", reconstructed.getInvoiceFooter());
        assertEquals(30, reconstructed.getMinimumSessionMinutes());
        assertEquals(15, reconstructed.getBillingRoundingMinutes());
        assertEquals(
                "Microsoft Print to PDF",
                reconstructed.getReceiptPrintSettings().getSelectedPrinterName());
        assertEquals(
                ReceiptPaperWidth.MM_58,
                reconstructed.getReceiptPrintSettings().getPaperWidth());
        assertEquals(true, reconstructed.getReceiptPrintSettings().isAutoPrintAfterCheckout());
    }

    @Test
    void updatesExistingPricingAndCurrencyDisplayServicesWithoutRestart() {
        PricingService pricingService = new PricingService(
                new SettingsBillableDurationPolicy(settingsService),
                GamingPricePolicy.proratedHourlyRate(),
                MonetaryRoundingPolicy.standardCurrency());
        ApplicationDisplayService displayService =
                new ApplicationDisplayService(settingsService);

        updateSettings.execute(new ApplicationSettings(
                "Pixel Hub", "USD", "Thanks", 30, 15));

        PricingResult minimum = pricingService.calculate(
                START_TIME,
                START_TIME.plus(Duration.ofMinutes(5L)),
                new BigDecimal("120.00"));
        assertEquals(Duration.ofMinutes(30L), minimum.getBillableDuration());
        assertEquals(new BigDecimal("60.00"), minimum.getGamingPrice());

        PricingResult rounded = pricingService.calculate(
                START_TIME,
                START_TIME.plus(Duration.ofMinutes(31L)),
                new BigDecimal("120.00"));
        assertEquals(Duration.ofMinutes(45L), rounded.getBillableDuration());
        assertEquals(new BigDecimal("90.00"), rounded.getGamingPrice());
        assertEquals("USD 12.50", displayService.formatMoney(new BigDecimal("12.50")));

        updateSettings.execute(new ApplicationSettings(
                "Pixel Hub", "EUR", "Thanks", null, null));
        PricingResult exact = pricingService.calculate(
                START_TIME,
                START_TIME.plus(Duration.ofMinutes(31L)),
                new BigDecimal("120.00"));
        assertEquals(Duration.ofMinutes(31L), exact.getBillableDuration());
        assertEquals(new BigDecimal("62.00"), exact.getGamingPrice());
        assertEquals("EUR 12.50", displayService.formatMoney(new BigDecimal("12.50")));
    }

    @Test
    void validatesSettingsAndDeniesCashierAtApplicationBoundary() {
        assertThrows(ValidationException.class, () -> updateSettings.execute(
                new ApplicationSettings(" ", "", "Footer", 0, 1_441)));
        assertThrows(ValidationException.class, () -> updateSettings.execute(
                new ApplicationSettings(
                        "Game Cafe",
                        "EGP",
                        "Footer",
                        null,
                        null,
                        new ReceiptPrintSettings(null, ReceiptPaperWidth.MM_80, true))));

        SQLiteDatabase cashierDatabase = new SQLiteDatabase(
                temporaryDirectory.resolve("cashier-settings.db"));
        cashierDatabase.initialize();
        ApplicationSettingsService cashierSettingsService = new ApplicationSettingsService(
                new SQLiteSettingsRepository(cashierDatabase));
        AuthorizationService cashierAuthorization =
                AuthenticationTestSupport.authenticated(cashierDatabase, Role.CASHIER);

        assertThrows(AuthorizationException.class, () ->
                new GetSettingsUseCase(cashierSettingsService, cashierAuthorization).execute());
        assertThrows(AuthorizationException.class, () ->
                new UpdateSettingsUseCase(
                        cashierSettingsService,
                        new ApplicationSettingsValidator(),
                        cashierAuthorization).execute(ApplicationSettings.defaults()));
    }
}
