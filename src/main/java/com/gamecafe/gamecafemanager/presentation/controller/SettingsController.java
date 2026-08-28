package com.gamecafe.gamecafemanager.presentation.controller;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPaperWidth;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintSettings;
import com.gamecafe.gamecafemanager.domain.service.printing.ReceiptPrintingService;
import com.gamecafe.gamecafemanager.domain.usecase.settings.GetSettingsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.settings.UpdateSettingsUseCase;
import com.gamecafe.gamecafemanager.presentation.error.ApplicationErrorHandler;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public final class SettingsController {

    private final GetSettingsUseCase getSettingsUseCase;
    private final UpdateSettingsUseCase updateSettingsUseCase;
    private final ApplicationErrorHandler errorHandler;
    private final ReceiptPrintingService receiptPrintingService;
    private final Runnable onSettingsChanged;

    @FXML private TextField cafeNameField;
    @FXML private TextField currencyDisplayField;
    @FXML private TextArea invoiceFooterField;
    @FXML private TextField minimumDurationField;
    @FXML private TextField roundingMinutesField;
    @FXML private ComboBox<String> printerField;
    @FXML private ComboBox<ReceiptPaperWidth> receiptWidthField;
    @FXML private CheckBox autoPrintField;
    @FXML private Label printerStatusLabel;
    @FXML private Label savedLabel;

    public SettingsController(
            GetSettingsUseCase getSettingsUseCase,
            UpdateSettingsUseCase updateSettingsUseCase,
            ReceiptPrintingService receiptPrintingService,
            ApplicationErrorHandler errorHandler,
            Runnable onSettingsChanged) {
        this.getSettingsUseCase = Objects.requireNonNull(
                getSettingsUseCase, "getSettingsUseCase");
        this.updateSettingsUseCase = Objects.requireNonNull(
                updateSettingsUseCase, "updateSettingsUseCase");
        this.receiptPrintingService = Objects.requireNonNull(
                receiptPrintingService, "receiptPrintingService");
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
        this.onSettingsChanged = Objects.requireNonNull(
                onSettingsChanged, "onSettingsChanged");
    }

    @FXML
    private void initialize() {
        receiptWidthField.getItems().setAll(ReceiptPaperWidth.values());
        refresh();
    }

    @FXML
    private void save() {
        clearFeedback();
        try {
            ApplicationSettings saved = updateSettingsUseCase.execute(
                    new ApplicationSettings(
                            cafeNameField.getText(),
                            currencyDisplayField.getText(),
                            invoiceFooterField.getText(),
                            parseOptionalMinutes(
                                    minimumDurationField.getText(),
                                    "minimumSessionMinutes",
                                    "Minimum session duration"),
                            parseOptionalMinutes(
                                    roundingMinutesField.getText(),
                                    "billingRoundingMinutes",
                                    "Billing rounding interval"),
                            new ReceiptPrintSettings(
                                    printerField.getValue(),
                                    receiptWidthField.getValue() == null
                                            ? ReceiptPaperWidth.MM_80
                                            : receiptWidthField.getValue(),
                                    autoPrintField.isSelected())));
            populate(saved);
            onSettingsChanged.run();
            showFeedback("Settings saved.", false);
        } catch (ValidationException exception) {
            showFeedback("Check the highlighted settings and try again.", true);
            showValidationErrors(exception);
        } catch (RuntimeException exception) {
            showFeedback("Settings could not be saved.", true);
            showError("Could not save settings", exception);
        }
    }

    public void refresh() {
        showLoadingFeedback();
        try {
            populate(getSettingsUseCase.execute());
            clearFeedback();
        } catch (RuntimeException exception) {
            showFeedback("Settings could not be loaded.", true);
            showError("Could not load settings", exception);
        }
    }

    private void clearFeedback() {
        savedLabel.setText("");
        savedLabel.getStyleClass().removeAll(
                "feedback-success", "feedback-error", "secondary-text");
    }

    private void showLoadingFeedback() {
        clearFeedback();
        savedLabel.setText("Loading settings…");
        savedLabel.getStyleClass().add("secondary-text");
    }

    private void showFeedback(String message, boolean error) {
        savedLabel.setText(message);
        savedLabel.getStyleClass().removeAll(
                "feedback-success", "feedback-error", "secondary-text");
        savedLabel.getStyleClass().add(error ? "feedback-error" : "feedback-success");
    }

    private void populate(ApplicationSettings settings) {
        cafeNameField.setText(settings.getCafeName());
        currencyDisplayField.setText(settings.getCurrencyDisplay());
        invoiceFooterField.setText(settings.getInvoiceFooter());
        minimumDurationField.setText(formatOptional(settings.getMinimumSessionMinutes()));
        roundingMinutesField.setText(formatOptional(settings.getBillingRoundingMinutes()));
        ReceiptPrintSettings printSettings = settings.getReceiptPrintSettings();
        receiptWidthField.setValue(printSettings.getPaperWidth());
        autoPrintField.setSelected(printSettings.isAutoPrintAfterCheckout());
        loadPrinterChoices(printSettings.getSelectedPrinterName());
    }

    @FXML
    private void refreshPrinters() {
        loadPrinterChoices(printerField.getValue());
    }

    @FXML
    private void clearPrinter() {
        printerField.getSelectionModel().clearSelection();
        printerField.setValue(null);
        updatePrinterStatus(null, printerField.getItems().isEmpty());
    }

    private void loadPrinterChoices(String selectedPrinter) {
        try {
            printerField.getItems().setAll(receiptPrintingService.discoverPrinters());
            if (selectedPrinter == null || selectedPrinter.trim().isEmpty()) {
                printerField.getSelectionModel().clearSelection();
                printerField.setValue(null);
            } else {
                printerField.setValue(selectedPrinter.trim());
            }
            updatePrinterStatus(selectedPrinter, printerField.getItems().isEmpty());
        } catch (RuntimeException exception) {
            printerField.getItems().clear();
            printerField.setValue(selectedPrinter);
            errorHandler.handle(exception, "Could not discover printers");
            printerStatusLabel.setText(
                    "Printer discovery failed. Check Windows printer settings and retry.");
            printerStatusLabel.getStyleClass().removeAll(
                    "feedback-success", "secondary-text");
            printerStatusLabel.getStyleClass().add("feedback-error");
        }
    }

    private void updatePrinterStatus(String selectedPrinter, boolean noPrinters) {
        printerStatusLabel.getStyleClass().removeAll(
                "feedback-success", "feedback-error", "secondary-text");
        if (noPrinters) {
            printerStatusLabel.setText("No Windows printers are currently available.");
            printerStatusLabel.getStyleClass().add("feedback-error");
            return;
        }
        if (selectedPrinter == null || selectedPrinter.trim().isEmpty()) {
            printerStatusLabel.setText("No receipt printer configured. Manual selection remains available.");
            printerStatusLabel.getStyleClass().add("secondary-text");
            return;
        }
        boolean installed = printerField.getItems().stream()
                .anyMatch(name -> name.equalsIgnoreCase(selectedPrinter.trim()));
        if (installed) {
            printerStatusLabel.setText("Printer is available in Windows.");
            printerStatusLabel.getStyleClass().add("feedback-success");
        } else {
            printerStatusLabel.setText(
                    "The configured printer is missing. Select another printer or clear it.");
            printerStatusLabel.getStyleClass().add("feedback-error");
        }
    }

    private Integer parseOptionalMinutes(String text, String field, String label) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(normalized);
        } catch (NumberFormatException exception) {
            throw ValidationException.forField(field, label + " must be a whole number");
        }
    }

    private String formatOptional(Integer value) {
        return value == null ? "" : value.toString();
    }

    private void showValidationErrors(ValidationException exception) {
        showError("Check settings", exception);
    }

    private void showError(String title, Throwable failure) {
        errorHandler.show(
                cafeNameField.getScene() == null
                        ? null
                        : cafeNameField.getScene().getWindow(),
                title,
                failure);
    }
}
