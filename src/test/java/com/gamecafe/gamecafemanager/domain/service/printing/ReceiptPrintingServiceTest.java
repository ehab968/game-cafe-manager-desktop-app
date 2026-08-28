package com.gamecafe.gamecafemanager.domain.service.printing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.model.Invoice;
import com.gamecafe.gamecafemanager.domain.model.ReceiptDocument;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPaperWidth;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintMode;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintResult;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintSettings;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintStatus;
import com.gamecafe.gamecafemanager.domain.model.SessionMode;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReceiptPrintingServiceTest {

    @Test
    void reportsNoPrintersWithoutAttemptingPrint() {
        FakePrinter printer = new FakePrinter(Collections.emptyList());
        ReceiptPrintingService service = service(settings("POS-80", true), printer);

        ReceiptPrintResult result = service.printConfigured(invoice());

        assertEquals(ReceiptPrintStatus.NO_PRINTERS_AVAILABLE, result.getStatus());
        assertEquals(0, printer.printCount);
    }

    @Test
    void reportsMissingConfiguredPrinterWithoutPrintingToAnotherDevice() {
        FakePrinter printer = new FakePrinter(Collections.singletonList("Microsoft Print to PDF"));
        ReceiptPrintingService service = service(settings("POS-80", true), printer);

        ReceiptPrintResult result = service.printConfigured(invoice());

        assertEquals(ReceiptPrintStatus.PRINTER_NOT_FOUND, result.getStatus());
        assertEquals(0, printer.printCount);
    }

    @Test
    void autoPrintDisabledDoesNotDiscoverRenderOrPrint() {
        FakePrinter printer = new FakePrinter(Collections.singletonList("POS-80"));
        ReceiptPrintingService service = service(settings("POS-80", false), printer);

        ReceiptPrintResult result = service.autoPrint(invoice());

        assertEquals(ReceiptPrintStatus.AUTO_PRINT_DISABLED, result.getStatus());
        assertEquals(0, printer.discoveryCount);
        assertEquals(0, printer.printCount);
    }

    @Test
    void autoPrintEnabledUsesConfiguredPrinterAndWidth() {
        FakePrinter printer = new FakePrinter(Collections.singletonList("POS-80"));
        ReceiptPrintingService service = service(settings("POS-80", true), printer);

        ReceiptPrintResult result = service.autoPrint(invoice());

        assertEquals(ReceiptPrintStatus.PRINTED, result.getStatus());
        assertEquals(1, printer.printCount);
        assertEquals("POS-80", printer.lastPrinterName);
        assertEquals(ReceiptPaperWidth.MM_58, printer.lastReceipt.getPaperWidth());
        assertEquals(ReceiptPrintMode.AUTOMATIC, printer.lastPrintMode);
    }

    @Test
    void printFailureIsReturnedWithoutChangingInvoice() {
        FakePrinter printer = new FakePrinter(Collections.singletonList("POS-80"));
        printer.printResult = ReceiptPrintResult.of(
                ReceiptPrintStatus.FAILED, "Simulated spooler failure");
        ReceiptPrintingService service = service(settings("POS-80", true), printer);
        Invoice invoice = invoice();

        ReceiptPrintResult result = service.autoPrint(invoice);

        assertEquals(ReceiptPrintStatus.FAILED, result.getStatus());
        assertEquals(new BigDecimal("80.00"), invoice.getHourlyRateSnapshot());
        assertEquals(new BigDecimal("120.00"), invoice.getTotal());
    }

    @Test
    void manualPrintAndPreviewCanUseAChosenPrinterWithoutSavingSettings() {
        FakePrinter printer = new FakePrinter(Collections.singletonList("PDF"));
        ReceiptPrintingService service = service(settings(null, false), printer);

        ReceiptDocument preview = service.preview(invoice(), ReceiptPaperWidth.MM_80);
        ReceiptPrintResult result = service.print(
                invoice(), "PDF", ReceiptPaperWidth.MM_80);

        assertEquals(ReceiptPaperWidth.MM_80, preview.getPaperWidth());
        assertEquals(ReceiptPrintStatus.PRINTED, result.getStatus());
        assertEquals("PDF", printer.lastPrinterName);
        assertEquals(ReceiptPrintMode.MANUAL, printer.lastPrintMode);
        assertTrue(printer.lastReceipt.asPlainText().contains("Mode: Multi"));
        assertFalse(service.getPrintSettings().hasSelectedPrinter());
    }

    private ReceiptPrintingService service(
            ApplicationSettings settings,
            FakePrinter printer) {
        return new ReceiptPrintingService(
                () -> settings,
                new ReceiptRenderer(ZoneOffset.UTC),
                printer);
    }

    private ApplicationSettings settings(String printerName, boolean autoPrint) {
        return new ApplicationSettings(
                "Circle Game",
                "EGP",
                "Thanks",
                null,
                null,
                new ReceiptPrintSettings(
                        printerName,
                        ReceiptPaperWidth.MM_58,
                        autoPrint));
    }

    private Invoice invoice() {
        Instant start = Instant.parse("2026-08-28T18:00:00Z");
        Instant end = start.plusSeconds(3_600L);
        return new Invoice(
                "Circle Game",
                "EGP",
                "Thanks",
                "INV-000001",
                1L,
                "Room 1",
                StationType.PLAYSTATION,
                SessionMode.MULTI,
                new BigDecimal("80.00"),
                start,
                end,
                Duration.ofHours(1L),
                new BigDecimal("80.00"),
                Collections.emptyList(),
                new BigDecimal("40.00"),
                new BigDecimal("120.00"));
    }

    private static final class FakePrinter implements ReceiptPrinter {

        private final List<String> names;
        private int discoveryCount;
        private int printCount;
        private String lastPrinterName;
        private ReceiptPrintMode lastPrintMode;
        private ReceiptDocument lastReceipt;
        private ReceiptPrintResult printResult = ReceiptPrintResult.of(
                ReceiptPrintStatus.PRINTED, "Printed");

        private FakePrinter(List<String> names) {
            this.names = new ArrayList<>(names);
        }

        @Override
        public List<String> discoverPrinterNames() {
            discoveryCount++;
            return new ArrayList<>(names);
        }

        @Override
        public ReceiptPrintResult print(
                ReceiptDocument receipt,
                String printerName,
                ReceiptPrintMode printMode) {
            printCount++;
            lastReceipt = receipt;
            lastPrinterName = printerName;
            lastPrintMode = printMode;
            return printResult;
        }
    }
}
