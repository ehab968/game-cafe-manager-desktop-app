package com.gamecafe.gamecafemanager.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ReceiptDocument {

    private final ReceiptPaperWidth paperWidth;
    private final List<ReceiptLine> lines;

    public ReceiptDocument(ReceiptPaperWidth paperWidth, List<ReceiptLine> lines) {
        this.paperWidth = Objects.requireNonNull(paperWidth, "paperWidth");
        this.lines = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(lines, "lines")));
    }

    public ReceiptPaperWidth getPaperWidth() {
        return paperWidth;
    }

    public List<ReceiptLine> getLines() {
        return lines;
    }

    public String asPlainText() {
        return lines.stream()
                .map(ReceiptLine::getText)
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
