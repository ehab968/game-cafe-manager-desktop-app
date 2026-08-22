package com.gamecafe.gamecafemanager.presentation.controller;

import com.gamecafe.gamecafemanager.domain.model.Invoice;
import com.gamecafe.gamecafemanager.domain.model.InvoiceItem;
import com.gamecafe.gamecafemanager.presentation.format.ApplicationDisplayService;
import com.gamecafe.gamecafemanager.presentation.style.UiStyles;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * JavaFX adapter for previewing an output-neutral invoice document.
 */
public final class InvoicePreviewDialog {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    public void show(Invoice invoice, Window owner) {
        Objects.requireNonNull(invoice, "invoice");
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
        details.addRow(5, new Label("Start time"),
                new Label(TIME_FORMAT.format(invoice.getStartTime())));
        details.addRow(6, new Label("End time"),
                new Label(TIME_FORMAT.format(invoice.getEndTime())));
        details.addRow(7, new Label("Duration"),
                new Label(formatDuration(invoice.getDuration())));
        details.addRow(8, new Label("Gaming amount"),
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
        totals.addRow(0, new Label("Products amount"),
                new Label(formatMoney(invoice, invoice.getProductsTotal())));
        Label total = new Label(formatMoney(invoice, invoice.getTotal()));
        total.getStyleClass().add("invoice-total");
        totals.addRow(1, new Label("Total"), total);

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
        content.setPadding(new Insets(4.0));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(580.0);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
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
