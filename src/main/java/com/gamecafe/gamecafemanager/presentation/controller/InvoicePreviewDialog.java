package com.gamecafe.gamecafemanager.presentation.controller;

import com.gamecafe.gamecafemanager.domain.model.Invoice;
import com.gamecafe.gamecafemanager.domain.model.InvoiceItem;
import com.gamecafe.gamecafemanager.domain.model.ReceiptDocument;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintResult;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintSettings;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintStatus;
import com.gamecafe.gamecafemanager.domain.service.printing.ReceiptPrintingService;
import com.gamecafe.gamecafemanager.presentation.error.ApplicationErrorHandler;
import com.gamecafe.gamecafemanager.presentation.format.ApplicationDisplayService;
import com.gamecafe.gamecafemanager.presentation.style.UiStyles;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Window;

/**
 * JavaFX adapter for previewing an output-neutral invoice and printing its
 * persisted values as a thermal receipt.
 */
public final class InvoicePreviewDialog {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private final ReceiptPrintingService receiptPrintingService;
    private final ApplicationErrorHandler errorHandler;

    public InvoicePreviewDialog(
            ReceiptPrintingService receiptPrintingService,
            ApplicationErrorHandler errorHandler) {
        this.receiptPrintingService = Objects.requireNonNull(
                receiptPrintingService, "receiptPrintingService");
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
    }

    public void show(Invoice invoice, Window owner) {
        show(invoice, owner, null);
    }

    public void show(
            Invoice invoice,
            Window owner,
            ReceiptPrintResult autoPrintResult) {
        Objects.requireNonNull(invoice, "invoice");
        showAutoPrintOutcome(autoPrintResult, owner);

        Dialog<Void> dialog = new Dialog<>();
        UiStyles.apply(dialog.getDialogPane());
        dialog.setTitle(invoice.getCafeName() + " — Invoice preview");
        dialog.setHeaderText("Invoice " + invoice.getInvoiceNumber());
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setResizable(true);

        GridPane details = new GridPane();
        details.setHgap(18.0);
        details.setVgap(8.0);
        details.addRow(0, new Label("Cafe"), new Label(invoice.getCafeName()));
        details.addRow(1, new Label("Invoice number"), new Label(invoice.getInvoiceNumber()));
        details.addRow(2, new Label("Session number"),
                new Label(Long.toString(invoice.getSessionId())));
        details.addRow(3, new Label("Station"), new Label(invoice.getStationName()));
        details.addRow(4, new Label("Station type"),
                new Label(invoice.getStationType().getDisplayName()));
        int detailRow = 5;
        if (invoice.getMode() != null) {
            details.addRow(
                    detailRow++,
                    new Label("Mode"),
                    new Label(invoice.getMode().getDisplayName()));
        }
        details.addRow(
                detailRow++,
                new Label("Hourly rate"),
                new Label(formatMoney(invoice, invoice.getHourlyRateSnapshot()) + "/hour"));
        details.addRow(detailRow++, new Label("Start time"),
                new Label(TIME_FORMAT.format(invoice.getStartTime())));
        details.addRow(detailRow++, new Label("End time"),
                new Label(TIME_FORMAT.format(invoice.getEndTime())));
        details.addRow(detailRow++, new Label("Duration"),
                new Label(formatDuration(invoice.getDuration())));
        details.addRow(detailRow, new Label("Gaming amount"),
                new Label(formatMoney(invoice, invoice.getGamingAmount())));

        VBox productLines = new VBox(6.0);
        if (invoice.getPurchasedProducts().isEmpty()) {
            productLines.getChildren().add(new Label("No products purchased"));
        } else {
            for (InvoiceItem item : invoice.getPurchasedProducts()) {
                productLines.getChildren().add(new Label(
                        item.getQuantity() + " × " + item.getProductName()
                                + " @ " + formatMoney(invoice, item.getUnitPrice())
                                + " = " + formatMoney(invoice, item.getLineTotal())));
            }
        }
        ScrollPane productsPane = new ScrollPane(productLines);
        productsPane.setFitToWidth(true);
        productsPane.setPrefViewportHeight(Math.min(
                170.0, 34.0 + invoice.getPurchasedProducts().size() * 28.0));

        GridPane totals = new GridPane();
        totals.setHgap(18.0);
        totals.setVgap(8.0);
        int totalsRow = 0;
        if (invoice.getGamingDiscount().isApplied()) {
            totals.addRow(
                    totalsRow++,
                    new Label("Gaming discount ("
                            + invoice.getGamingDiscount().getPercentage() + "%)"),
                    new Label("-" + formatMoney(
                            invoice, invoice.getGamingDiscountAmount())));
            totals.addRow(
                    totalsRow++,
                    new Label("Gaming after discount"),
                    new Label(formatMoney(
                            invoice, invoice.getDiscountedGamingAmount())));
        }
        totals.addRow(totalsRow++, new Label("Products amount"),
                new Label(formatMoney(invoice, invoice.getProductsTotal())));
        Label total = new Label(formatMoney(invoice, invoice.getTotal()));
        total.getStyleClass().add("invoice-total");
        totals.addRow(totalsRow, new Label("Total"), total);

        VBox content = new VBox(12.0);
        content.getChildren().addAll(
                details,
                new Separator(),
                new Label("Purchased products"),
                productsPane,
                new Separator(),
                totals);
        if (!invoice.getFooter().isEmpty()) {
            Label footer = new Label(invoice.getFooter());
            footer.setWrapText(true);
            footer.getStyleClass().add("invoice-footer");
            content.getChildren().addAll(new Separator(), footer);
        }
        if (autoPrintResult != null && autoPrintResult.isPrinted()) {
            Label printStatus = new Label(autoPrintResult.getMessage());
            printStatus.getStyleClass().add("feedback-success");
            content.getChildren().addAll(new Separator(), printStatus);
        }
        content.setPadding(new Insets(4.0));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(580.0);

        ButtonType printType = new ButtonType(
                "Print receipt", ButtonBar.ButtonData.APPLY);
        ButtonType receiptPreviewType = new ButtonType(
                "Receipt preview", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().setAll(
                printType, receiptPreviewType, ButtonType.CLOSE);
        dialog.setOnShown(event -> {
            Button printButton = (Button) dialog.getDialogPane().lookupButton(printType);
            printButton.getStyleClass().add("primary-button");
            printButton.addEventFilter(ActionEvent.ACTION, action -> {
                action.consume();
                printReceipt(invoice, dialog.getDialogPane().getScene().getWindow());
            });
            Button previewButton =
                    (Button) dialog.getDialogPane().lookupButton(receiptPreviewType);
            previewButton.addEventFilter(ActionEvent.ACTION, action -> {
                action.consume();
                showReceiptPreview(invoice, dialog.getDialogPane().getScene().getWindow());
            });
        });
        dialog.showAndWait();
    }

    private void printReceipt(Invoice invoice, Window owner) {
        try {
            ReceiptPrintResult result = receiptPrintingService.printConfigured(invoice);
            if (result.getStatus() == ReceiptPrintStatus.PRINTER_NOT_CONFIGURED
                    || result.getStatus() == ReceiptPrintStatus.PRINTER_NOT_FOUND) {
                Optional<String> selectedPrinter = choosePrinter(owner, result.getMessage());
                if (!selectedPrinter.isPresent()) {
                    return;
                }
                ReceiptPrintSettings settings = receiptPrintingService.getPrintSettings();
                result = receiptPrintingService.print(
                        invoice,
                        selectedPrinter.get(),
                        settings.getPaperWidth());
            }
            showPrintResult(result, owner);
        } catch (RuntimeException exception) {
            errorHandler.show(owner, "Could not print receipt", exception);
        }
    }

    private Optional<String> choosePrinter(Window owner, String reason) {
        List<String> printers = receiptPrintingService.discoverPrinters();
        if (printers.isEmpty()) {
            showPrintResult(
                    ReceiptPrintResult.of(
                            ReceiptPrintStatus.NO_PRINTERS_AVAILABLE,
                            "No printers are installed or available in Windows."),
                    owner);
            return Optional.empty();
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(printers.get(0), printers);
        UiStyles.apply(dialog.getDialogPane());
        dialog.setTitle("Select receipt printer");
        dialog.setHeaderText("Choose a Windows printer");
        dialog.setContentText(reason + System.lineSeparator() + "Printer:");
        if (owner != null) {
            dialog.initOwner(owner);
        }
        return dialog.showAndWait();
    }

    private void showReceiptPreview(Invoice invoice, Window owner) {
        ReceiptDocument receipt = receiptPrintingService.previewConfigured(invoice);
        Dialog<Void> preview = new Dialog<>();
        UiStyles.apply(preview.getDialogPane());
        preview.setTitle(invoice.getCafeName() + " — Receipt preview");
        preview.setHeaderText(
                receipt.getPaperWidth().getDisplayName() + " thermal receipt");
        if (owner != null) {
            preview.initOwner(owner);
        }
        TextArea receiptText = new TextArea(receipt.asPlainText());
        receiptText.setEditable(false);
        receiptText.setWrapText(false);
        receiptText.setFont(Font.font("Monospaced", 13.0));
        receiptText.setPrefColumnCount(receipt.getPaperWidth().getCharacterColumns() + 2);
        receiptText.setPrefRowCount(Math.min(30, Math.max(14, receipt.getLines().size())));
        VBox content = new VBox(
                8.0,
                new Label("Preview uses the same rendered content sent to the printer."),
                receiptText);
        content.setPadding(new Insets(4.0));
        preview.getDialogPane().setContent(content);
        preview.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        preview.setResizable(true);
        preview.showAndWait();
    }

    private void showAutoPrintOutcome(ReceiptPrintResult result, Window owner) {
        if (result == null || !result.isFailure()) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.WARNING);
        UiStyles.apply(alert.getDialogPane());
        alert.setTitle("Checkout completed");
        alert.setHeaderText("Checkout completed, but the receipt was not printed");
        alert.setContentText(result.getMessage() + System.lineSeparator()
                + "The invoice remains saved. Use Print receipt to retry.");
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    private void showPrintResult(ReceiptPrintResult result, Window owner) {
        Alert.AlertType type = result.isPrinted()
                ? Alert.AlertType.INFORMATION
                : result.getStatus() == ReceiptPrintStatus.CANCELLED
                        ? Alert.AlertType.INFORMATION
                        : Alert.AlertType.WARNING;
        Alert alert = new Alert(type);
        UiStyles.apply(alert.getDialogPane());
        alert.setTitle("Receipt printing");
        alert.setHeaderText(result.isPrinted() ? "Receipt sent" : "Receipt not printed");
        alert.setContentText(result.getMessage());
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    private String formatMoney(Invoice invoice, java.math.BigDecimal amount) {
        return ApplicationDisplayService.formatMoney(
                amount, invoice.getCurrencyDisplay());
    }

    private String formatDuration(Duration duration) {
        long totalSeconds = duration.getSeconds();
        long hours = totalSeconds / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
