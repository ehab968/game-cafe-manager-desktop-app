package com.gamecafe.gamecafemanager.domain.service.printing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamecafe.gamecafemanager.domain.model.Invoice;
import com.gamecafe.gamecafemanager.domain.model.InvoiceItem;
import com.gamecafe.gamecafemanager.domain.model.ReceiptDocument;
import com.gamecafe.gamecafemanager.domain.model.ReceiptLine;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPaperWidth;
import com.gamecafe.gamecafemanager.domain.model.SessionMode;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReceiptRendererTest {

    private static final Instant START = Instant.parse("2026-08-28T19:00:00Z");
    private static final Instant END = START.plus(Duration.ofMinutes(90L));
    private final ReceiptRenderer renderer = new ReceiptRenderer(ZoneOffset.UTC);

    @Test
    void rendersEightyMillimeterReceiptWithinItsColumnWidth() {
        ReceiptDocument receipt = renderer.render(
                invoice(StationType.PLAYSTATION, SessionMode.MULTI, "PS5 Room 1"),
                ReceiptPaperWidth.MM_80);

        assertAllLinesFit(receipt, 42);
        assertContains(receipt, "Invoice: INV-000123");
        assertContains(receipt, "Date: 28/08/2026 08:30 PM");
        assertContains(receipt, "Station: PS5 Room 1");
        assertContains(receipt, "Mode: Multi");
    }

    @Test
    void rendersFiftyEightMillimeterReceiptWithSafeWrapping() {
        ReceiptDocument receipt = renderer.render(
                invoice(
                        StationType.PING_PONG,
                        SessionMode.SINGLE,
                        "Very Long Ping Pong Tournament Table Name"),
                ReceiptPaperWidth.MM_58);

        assertAllLinesFit(receipt, 32);
        assertContains(receipt, "Mode: Single");
        assertTrue(receipt.getLines().stream()
                .filter(line -> line.getText().contains("Very Long Ping Pong"))
                .count() >= 1L);
    }

    @Test
    void rendersPlayStationSingleAndMultiModesFromInvoiceSnapshot() {
        ReceiptDocument single = renderer.render(
                invoice(StationType.PLAYSTATION, SessionMode.SINGLE, "Room 1"),
                ReceiptPaperWidth.MM_80);
        ReceiptDocument multi = renderer.render(
                invoice(StationType.PLAYSTATION, SessionMode.MULTI, "Room 1"),
                ReceiptPaperWidth.MM_80);

        assertContains(single, "Mode: Single");
        assertContains(multi, "Mode: Multi");
        assertContains(multi, "Rate: 80.00 EGP/hour");
    }

    @Test
    void rendersPingPongSingleAndMultiModes() {
        ReceiptDocument single = renderer.render(
                invoice(StationType.PING_PONG, SessionMode.SINGLE, "Table 1"),
                ReceiptPaperWidth.MM_58);
        ReceiptDocument multi = renderer.render(
                invoice(StationType.PING_PONG, SessionMode.MULTI, "Table 1"),
                ReceiptPaperWidth.MM_58);

        assertContains(single, "Mode: Single");
        assertContains(multi, "Mode: Multi");
    }

    @Test
    void billiardReceiptDoesNotShowMeaninglessMode() {
        ReceiptDocument receipt = renderer.render(
                invoice(StationType.BILLIARD, null, "Billiard 1"),
                ReceiptPaperWidth.MM_80);

        assertFalse(receipt.asPlainText().contains("Mode:"));
        assertContains(receipt, "Type: Billiard table");
    }

    @Test
    void productLinesAndTotalsUsePersistedInvoiceValuesWithoutRecalculation() {
        ReceiptDocument receipt = renderer.render(
                invoice(StationType.PLAYSTATION, SessionMode.MULTI, "Room 1"),
                ReceiptPaperWidth.MM_80);

        assertContains(receipt, "Pepsi x2");
        assertContains(receipt, "@ 25.00 EGP");
        assertContains(receipt, "50.00 EGP");
        assertContains(receipt, "Gaming");
        assertContains(receipt, "37.13 EGP");
        assertContains(receipt, "Products");
        assertContains(receipt, "50.00 EGP");
        assertContains(receipt, "87.13 EGP");
    }

    @Test
    void historicalHourlyRateSnapshotIsPrintedExactly() {
        Invoice historicInvoice = invoice(
                StationType.PLAYSTATION, SessionMode.MULTI, "Historic Room");

        ReceiptDocument receipt = renderer.render(
                historicInvoice, ReceiptPaperWidth.MM_80);

        assertContains(receipt, "Rate: 80.00 EGP/hour");
    }

    private Invoice invoice(StationType type, SessionMode mode, String stationName) {
        List<InvoiceItem> items = Collections.singletonList(new InvoiceItem(
                7L,
                "Pepsi",
                new BigDecimal("25.00"),
                2,
                new BigDecimal("50.00")));
        return new Invoice(
                "Circle Game",
                "EGP",
                "Thank You",
                "INV-000123",
                123L,
                stationName,
                type,
                mode,
                new BigDecimal("80.00"),
                START,
                END,
                Duration.between(START, END),
                new BigDecimal("37.13"),
                items,
                new BigDecimal("50.00"),
                new BigDecimal("87.13"));
    }

    private void assertAllLinesFit(ReceiptDocument receipt, int columns) {
        assertTrue(receipt.getLines().stream()
                .map(ReceiptLine::getText)
                .allMatch(line -> line.length() <= columns));
    }

    private void assertContains(ReceiptDocument receipt, String expected) {
        assertTrue(
                receipt.asPlainText().contains(expected),
                () -> "Expected receipt to contain: " + expected
                        + System.lineSeparator() + receipt.asPlainText());
    }
}
