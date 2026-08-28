package com.gamecafe.gamecafemanager.domain.model;

import java.util.Objects;

public final class ReceiptLine {

    private final String text;
    private final boolean emphasized;

    public ReceiptLine(String text, boolean emphasized) {
        this.text = Objects.requireNonNull(text, "text");
        this.emphasized = emphasized;
    }

    public static ReceiptLine normal(String text) {
        return new ReceiptLine(text, false);
    }

    public static ReceiptLine emphasized(String text) {
        return new ReceiptLine(text, true);
    }

    public String getText() {
        return text;
    }

    public boolean isEmphasized() {
        return emphasized;
    }
}
