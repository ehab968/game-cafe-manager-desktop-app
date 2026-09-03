package com.gamecafe.gamecafemanager.domain.service.printing;

import com.gamecafe.gamecafemanager.domain.model.Invoice;
import com.gamecafe.gamecafemanager.domain.model.InvoiceItem;
import com.gamecafe.gamecafemanager.domain.model.ReceiptDocument;
import com.gamecafe.gamecafemanager.domain.model.ReceiptLine;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPaperWidth;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Converts persisted invoice snapshots into width-aware, output-neutral receipt
 * lines. It formats values but never recalculates invoice totals.
 */
public final class ReceiptRenderer {

    private final DateTimeFormatter dateTimeFormatter;

    public ReceiptRenderer() {
        this(ZoneId.systemDefault());
    }

    public ReceiptRenderer(ZoneId zoneId) {
        dateTimeFormatter = DateTimeFormatter.ofPattern(
                "dd/MM/uuuu hh:mm a", Locale.ENGLISH)
                .withZone(Objects.requireNonNull(zoneId, "zoneId"));
    }

    public ReceiptDocument render(Invoice invoice, ReceiptPaperWidth paperWidth) {
        Objects.requireNonNull(invoice, "invoice");
        Objects.requireNonNull(paperWidth, "paperWidth");
        int columns = paperWidth.getCharacterColumns();
        List<ReceiptLine> lines = new ArrayList<>();

        addCenteredWrapped(lines, invoice.getCafeName(), columns, true);
        lines.add(ReceiptLine.normal(repeat('-', columns)));
        addWrapped(lines, "Invoice: " + invoice.getInvoiceNumber(), columns, false);
        addWrapped(lines, "Date: " + dateTimeFormatter.format(invoice.getEndTime()), columns, false);
        lines.add(ReceiptLine.normal(""));
        addWrapped(lines, "Station: " + invoice.getStationName(), columns, false);
        addWrapped(lines, "Type: " + invoice.getStationType().getDisplayName(), columns, false);
        if (invoice.getMode() != null) {
            addWrapped(lines, "Mode: " + invoice.getMode().getDisplayName(), columns, false);
        }
        addWrapped(
                lines,
                "Rate: " + formatMoney(invoice.getHourlyRateSnapshot(), invoice)
                        + "/hour",
                columns,
                false);
        addWrapped(lines, "Start: " + dateTimeFormatter.format(invoice.getStartTime()), columns, false);
        addWrapped(lines, "End: " + dateTimeFormatter.format(invoice.getEndTime()), columns, false);
        addWrapped(lines, "Duration: " + formatDuration(invoice.getDuration()), columns, false);
        lines.add(ReceiptLine.normal(""));
        lines.add(ReceiptLine.normal(repeat('-', columns)));
        addAmountLine(
                lines,
                "Gaming",
                formatMoney(invoice.getGamingAmount(), invoice),
                columns,
                false);
        if (invoice.getGamingDiscount().isApplied()) {
            addAmountLine(
                    lines,
                    "Discount " + invoice.getGamingDiscount().getPercentage() + "%",
                    "-" + formatMoney(invoice.getGamingDiscountAmount(), invoice),
                    columns,
                    false);
            addAmountLine(
                    lines,
                    "Gaming after discount",
                    formatMoney(invoice.getDiscountedGamingAmount(), invoice),
                    columns,
                    false);
        }

        for (InvoiceItem item : invoice.getPurchasedProducts()) {
            addWrapped(
                    lines,
                    item.getProductName() + " x" + item.getQuantity(),
                    columns,
                    false);
            addAmountLine(
                    lines,
                    "@ " + formatMoney(item.getUnitPrice(), invoice),
                    formatMoney(item.getLineTotal(), invoice),
                    columns,
                    false);
        }

        if (!invoice.getPurchasedProducts().isEmpty()) {
            lines.add(ReceiptLine.normal(""));
        }
        addAmountLine(
                lines,
                "Products",
                formatMoney(invoice.getProductsTotal(), invoice),
                columns,
                false);
        lines.add(ReceiptLine.normal(repeat('-', columns)));
        addAmountLine(
                lines,
                "TOTAL",
                formatMoney(invoice.getTotal(), invoice),
                columns,
                true);
        lines.add(ReceiptLine.emphasized(repeat('=', columns)));

        if (!invoice.getFooter().trim().isEmpty()) {
            lines.add(ReceiptLine.normal(""));
            addCenteredWrapped(lines, invoice.getFooter(), columns, false);
        }
        return new ReceiptDocument(paperWidth, lines);
    }

    private void addAmountLine(
            List<ReceiptLine> lines,
            String label,
            String amount,
            int columns,
            boolean emphasized) {
        String normalizedLabel = label.trim();
        String normalizedAmount = amount.trim();
        int spaces = columns - normalizedLabel.length() - normalizedAmount.length();
        if (spaces >= 1) {
            addLine(
                    lines,
                    normalizedLabel + repeat(' ', spaces) + normalizedAmount,
                    emphasized);
            return;
        }
        addWrapped(lines, normalizedLabel, columns, emphasized);
        addLine(lines, rightAlign(normalizedAmount, columns), emphasized);
    }

    private void addCenteredWrapped(
            List<ReceiptLine> lines,
            String text,
            int columns,
            boolean emphasized) {
        for (String line : wrap(text, columns)) {
            addLine(lines, center(line, columns), emphasized);
        }
    }

    private void addWrapped(
            List<ReceiptLine> lines,
            String text,
            int columns,
            boolean emphasized) {
        for (String line : wrap(text, columns)) {
            addLine(lines, line, emphasized);
        }
    }

    private void addLine(List<ReceiptLine> lines, String text, boolean emphasized) {
        lines.add(emphasized
                ? ReceiptLine.emphasized(text)
                : ReceiptLine.normal(text));
    }

    private List<String> wrap(String text, int columns) {
        List<String> wrapped = new ArrayList<>();
        String normalized = Objects.requireNonNull(text, "text")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();
        if (normalized.isEmpty()) {
            wrapped.add("");
            return wrapped;
        }
        for (String paragraph : normalized.split("\n", -1)) {
            wrapParagraph(paragraph.trim(), columns, wrapped);
        }
        return wrapped;
    }

    private void wrapParagraph(String paragraph, int columns, List<String> wrapped) {
        String remaining = paragraph;
        if (remaining.isEmpty()) {
            wrapped.add("");
            return;
        }
        while (remaining.length() > columns) {
            int split = remaining.lastIndexOf(' ', columns);
            if (split <= 0) {
                split = columns;
            }
            wrapped.add(remaining.substring(0, split).trim());
            remaining = remaining.substring(split).trim();
        }
        wrapped.add(remaining);
    }

    private String center(String text, int columns) {
        if (text.length() >= columns) {
            return text;
        }
        int leftPadding = (columns - text.length()) / 2;
        return repeat(' ', leftPadding) + text;
    }

    private String rightAlign(String text, int columns) {
        if (text.length() >= columns) {
            return text;
        }
        return repeat(' ', columns - text.length()) + text;
    }

    private String formatMoney(BigDecimal value, Invoice invoice) {
        return value.setScale(2, RoundingMode.UNNECESSARY).toPlainString()
                + " " + invoice.getCurrencyDisplay();
    }

    private String formatDuration(Duration duration) {
        long totalSeconds = duration.getSeconds();
        long hours = totalSeconds / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String repeat(char character, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int index = 0; index < count; index++) {
            builder.append(character);
        }
        return builder.toString();
    }
}
