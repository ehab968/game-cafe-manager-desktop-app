package com.gamecafe.gamecafemanager.domain.service.printing;

import com.gamecafe.gamecafemanager.domain.model.ReceiptDocument;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintMode;
import com.gamecafe.gamecafemanager.domain.model.ReceiptPrintResult;
import java.util.List;

/**
 * Operating-system printing port. Implementations discover and address printers
 * by the names exposed by the host operating system.
 */
public interface ReceiptPrinter {

    List<String> discoverPrinterNames();

    ReceiptPrintResult print(
            ReceiptDocument receipt,
            String printerName,
            ReceiptPrintMode printMode);
}
