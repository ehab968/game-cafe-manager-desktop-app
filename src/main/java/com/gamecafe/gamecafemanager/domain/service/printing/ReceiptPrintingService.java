package com.gamecafe.gamecafemanager.domain.service.printing;

import com.gamecafe.gamecafemanager.domain.model.Invoice;
import com.gamecafe.gamecafemanager.domain.model.ReceiptDocument;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPaperWidth;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintMode;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintResult;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintSettings;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintStatus;
import com.gamecafe.gamecafemanager.domain.service.SettingsProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates rendering and operating-system printing without changing invoice,
 * checkout, stock, or session state.
 */
public final class ReceiptPrintingService {

    private final SettingsProvider settingsProvider;
    private final ReceiptRenderer renderer;
    private final ReceiptPrinter printer;

    public ReceiptPrintingService(
            SettingsProvider settingsProvider,
            ReceiptRenderer renderer,
            ReceiptPrinter printer) {
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.printer = Objects.requireNonNull(printer, "printer");
    }

    public List<String> discoverPrinters() {
        List<String> names = new ArrayList<>(printer.discoverPrinterNames());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return Collections.unmodifiableList(names);
    }

    public ReceiptPrintSettings getPrintSettings() {
        return settingsProvider.getSettings().getReceiptPrintSettings();
    }

    public ReceiptDocument preview(Invoice invoice, ReceiptPaperWidth paperWidth) {
        return renderer.render(invoice, paperWidth);
    }

    public ReceiptDocument previewConfigured(Invoice invoice) {
        return preview(invoice, getPrintSettings().getPaperWidth());
    }

    public ReceiptPrintResult print(
            Invoice invoice,
            String printerName,
            ReceiptPaperWidth paperWidth) {
        if (printerName == null || printerName.trim().isEmpty()) {
            return result(
                    ReceiptPrintStatus.PRINTER_NOT_CONFIGURED,
                    "Select a printer before printing the receipt.");
        }
        return printer.print(
                renderer.render(invoice, paperWidth),
                printerName.trim(),
                ReceiptPrintMode.MANUAL);
    }

    public ReceiptPrintResult printConfigured(Invoice invoice) {
        return printConfigured(invoice, getPrintSettings(), ReceiptPrintMode.MANUAL);
    }

    public ReceiptPrintResult autoPrint(Invoice invoice) {
        if (!getPrintSettings().isAutoPrintAfterCheckout()) {
            return result(
                    ReceiptPrintStatus.AUTO_PRINT_DISABLED,
                    "Automatic receipt printing is disabled.");
        }
        ReceiptPrintSettings settings = getPrintSettings();
        return printConfigured(invoice, settings, ReceiptPrintMode.AUTOMATIC);
    }

    private ReceiptPrintResult printConfigured(
            Invoice invoice,
            ReceiptPrintSettings settings,
            ReceiptPrintMode printMode) {
        List<String> availablePrinters = discoverPrinters();
        if (availablePrinters.isEmpty()) {
            return result(
                    ReceiptPrintStatus.NO_PRINTERS_AVAILABLE,
                    "No printers are installed or available in Windows.");
        }
        if (!settings.hasSelectedPrinter()) {
            return result(
                    ReceiptPrintStatus.PRINTER_NOT_CONFIGURED,
                    "No receipt printer is configured. Select one to continue.");
        }
        if (!containsPrinter(availablePrinters, settings.getSelectedPrinterName())) {
            return result(
                    ReceiptPrintStatus.PRINTER_NOT_FOUND,
                    "The configured printer is no longer available in Windows.");
        }
        return printer.print(
                renderer.render(invoice, settings.getPaperWidth()),
                settings.getSelectedPrinterName(),
                printMode);
    }

    private boolean containsPrinter(List<String> printers, String selectedPrinter) {
        return printers.stream().anyMatch(name -> name.equalsIgnoreCase(selectedPrinter));
    }

    private ReceiptPrintResult result(ReceiptPrintStatus status, String message) {
        return ReceiptPrintResult.of(status, message);
    }
}
