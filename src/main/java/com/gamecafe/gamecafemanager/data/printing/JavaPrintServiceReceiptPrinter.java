package com.gamecafe.gamecafemanager.data.printing;

import com.gamecafe.gamecafemanager.domain.exception.PrinterDiscoveryException;
import com.gamecafe.gamecafemanager.domain.model.ReceiptDocument;
import com.gamecafe.gamecafemanager.domain.model.ReceiptLine;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPaperWidth;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintMode;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintResult;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintStatus;
import com.gamecafe.gamecafemanager.domain.service.printing.ReceiptPrinter;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterAbortException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.print.PrintService;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.PrinterState;
import javax.print.attribute.standard.PrinterStateReason;
import javax.print.attribute.standard.PrinterStateReasons;

/**
 * Standard Java Print Service adapter. Windows printer drivers expose USB,
 * network, virtual, and thermal printers through this API without vendor SDKs.
 */
public final class JavaPrintServiceReceiptPrinter implements ReceiptPrinter {

    private static final Logger LOGGER =
            Logger.getLogger(JavaPrintServiceReceiptPrinter.class.getName());
    private static final double POINTS_PER_INCH = 72.0;
    private static final double MILLIMETERS_PER_INCH = 25.4;
    private static final double SIDE_MARGIN_MILLIMETERS = 2.0;
    private static final double MINIMUM_RECEIPT_HEIGHT_MILLIMETERS = 100.0;

    @Override
    public List<String> discoverPrinterNames() {
        try {
            return Arrays.stream(PrinterJob.lookupPrintServices())
                    .map(PrintService::getName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        } catch (RuntimeException exception) {
            throw new PrinterDiscoveryException(
                    "Windows printer discovery failed", exception);
        }
    }

    @Override
    public ReceiptPrintResult print(
            ReceiptDocument receipt,
            String printerName,
            ReceiptPrintMode printMode) {
        Objects.requireNonNull(receipt, "receipt");
        Objects.requireNonNull(printerName, "printerName");
        Objects.requireNonNull(printMode, "printMode");
        PrintService[] services;
        try {
            services = PrinterJob.lookupPrintServices();
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Could not query Windows printers", exception);
            return result(
                    ReceiptPrintStatus.FAILED,
                    "Windows printer information could not be read.");
        }
        if (services.length == 0) {
            return result(
                    ReceiptPrintStatus.NO_PRINTERS_AVAILABLE,
                    "No printers are installed or available in Windows.");
        }

        PrintService service = findPrinter(services, printerName);
        if (service == null) {
            return result(
                    ReceiptPrintStatus.PRINTER_NOT_FOUND,
                    "The selected printer is no longer available in Windows.");
        }
        if (isUnavailable(service)) {
            return result(
                    ReceiptPrintStatus.PRINTER_UNAVAILABLE,
                    "The selected printer is offline or not accepting print jobs.");
        }

        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(service);
            job.setJobName("GameCafe receipt");
            if (printMode == ReceiptPrintMode.MANUAL && !job.printDialog()) {
                return result(
                        ReceiptPrintStatus.CANCELLED,
                        "Receipt printing was cancelled.");
            }
            service = job.getPrintService();
            if (service == null || isUnavailable(service)) {
                return result(
                        ReceiptPrintStatus.PRINTER_UNAVAILABLE,
                        "The selected printer is offline or not accepting print jobs.");
            }
            PageFormat requestedPage = createReceiptPage(receipt);
            PageFormat validatedPage = job.validatePage(requestedPage);
            if (validatedPage.getImageableWidth() <= 0.0
                    || validatedPage.getImageableHeight() <= 0.0) {
                return result(
                        ReceiptPrintStatus.FAILED,
                        "The printer driver rejected the selected receipt paper layout.");
            }
            job.setPrintable(new ReceiptPrintable(receipt), validatedPage);
            job.print();
            return result(
                    ReceiptPrintStatus.PRINTED,
                    "Receipt sent to " + service.getName() + ".");
        } catch (PrinterAbortException exception) {
            LOGGER.log(Level.INFO, "Receipt printing was cancelled", exception);
            return result(
                    ReceiptPrintStatus.CANCELLED,
                    "Receipt printing was cancelled.");
        } catch (PrinterException | RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Receipt print job failed", exception);
            return result(
                    ReceiptPrintStatus.FAILED,
                    "The receipt could not be printed. Check the printer and try again.");
        }
    }

    private PrintService findPrinter(PrintService[] services, String printerName) {
        for (PrintService service : services) {
            if (service.getName().equalsIgnoreCase(printerName.trim())) {
                return service;
            }
        }
        return null;
    }

    private boolean isUnavailable(PrintService service) {
        PrinterIsAcceptingJobs accepting =
                service.getAttribute(PrinterIsAcceptingJobs.class);
        if (PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS.equals(accepting)) {
            return true;
        }
        PrinterState state = service.getAttribute(PrinterState.class);
        if (PrinterState.STOPPED.equals(state)) {
            return true;
        }
        PrinterStateReasons reasons = service.getAttribute(PrinterStateReasons.class);
        return reasons != null
                && (reasons.containsKey(PrinterStateReason.TIMED_OUT)
                        || reasons.containsKey(PrinterStateReason.SHUTDOWN)
                        || reasons.containsKey(PrinterStateReason.STOPPING));
    }

    private PageFormat createReceiptPage(ReceiptDocument receipt) {
        ReceiptPaperWidth paperWidth = receipt.getPaperWidth();
        double widthPoints = millimetersToPoints(paperWidth.getMillimeters());
        double marginPoints = millimetersToPoints(SIDE_MARGIN_MILLIMETERS);
        double estimatedLineHeight = paperWidth == ReceiptPaperWidth.MM_80 ? 11.0 : 10.0;
        double contentHeight = receipt.getLines().size() * estimatedLineHeight
                + marginPoints * 2.0 + 8.0;
        double heightPoints = Math.max(
                millimetersToPoints(MINIMUM_RECEIPT_HEIGHT_MILLIMETERS),
                contentHeight);

        Paper paper = new Paper();
        paper.setSize(widthPoints, heightPoints);
        paper.setImageableArea(
                marginPoints,
                marginPoints,
                widthPoints - marginPoints * 2.0,
                heightPoints - marginPoints * 2.0);
        PageFormat pageFormat = new PageFormat();
        pageFormat.setOrientation(PageFormat.PORTRAIT);
        pageFormat.setPaper(paper);
        return pageFormat;
    }

    private double millimetersToPoints(double millimeters) {
        return millimeters / MILLIMETERS_PER_INCH * POINTS_PER_INCH;
    }

    private ReceiptPrintResult result(ReceiptPrintStatus status, String message) {
        return ReceiptPrintResult.of(status, message);
    }

    private static final class ReceiptPrintable implements Printable {

        private final ReceiptDocument receipt;

        private ReceiptPrintable(ReceiptDocument receipt) {
            this.receipt = receipt;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                graphics2D.setColor(Color.BLACK);
                graphics2D.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int columns = receipt.getPaperWidth().getCharacterColumns();
                float preferredSize = receipt.getPaperWidth() == ReceiptPaperWidth.MM_80
                        ? 8.5f
                        : 7.5f;
                float widthLimitedSize = (float) (
                        pageFormat.getImageableWidth() / (columns * 0.62));
                float fontSize = Math.max(5.0f, Math.min(preferredSize, widthLimitedSize));
                Font plain = new Font(Font.MONOSPACED, Font.PLAIN, 1).deriveFont(fontSize);
                Font emphasized = plain.deriveFont(Font.BOLD);
                FontMetrics metrics = graphics2D.getFontMetrics(plain);
                int lineHeight = Math.max(1, metrics.getHeight() + 2);
                int linesPerPage = Math.max(
                        1,
                        (int) Math.floor(pageFormat.getImageableHeight() / lineHeight));
                int firstLine = pageIndex * linesPerPage;
                if (firstLine >= receipt.getLines().size()) {
                    return NO_SUCH_PAGE;
                }

                int lastLine = Math.min(
                        receipt.getLines().size(), firstLine + linesPerPage);
                float baseline = metrics.getAscent();
                for (int index = firstLine; index < lastLine; index++) {
                    ReceiptLine line = receipt.getLines().get(index);
                    graphics2D.setFont(line.isEmphasized() ? emphasized : plain);
                    graphics2D.drawString(line.getText(), 0.0f, baseline);
                    baseline += lineHeight;
                }
                return PAGE_EXISTS;
            } finally {
                graphics2D.dispose();
            }
        }
    }
}
