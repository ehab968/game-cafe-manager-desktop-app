package com.gamecafe.gamecafemanager.domain.model;

public enum ReceiptPaperWidth {
    MM_80(80, 42, "80 mm"),
    MM_58(58, 32, "58 mm");

    private final int millimeters;
    private final int characterColumns;
    private final String displayName;

    ReceiptPaperWidth(int millimeters, int characterColumns, String displayName) {
        this.millimeters = millimeters;
        this.characterColumns = characterColumns;
        this.displayName = displayName;
    }

    public int getMillimeters() {
        return millimeters;
    }

    public int getCharacterColumns() {
        return characterColumns;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ReceiptPaperWidth fromMillimeters(int millimeters) {
        for (ReceiptPaperWidth width : values()) {
            if (width.millimeters == millimeters) {
                return width;
            }
        }
        throw new IllegalArgumentException(
                "Unsupported receipt paper width: " + millimeters + " mm");
    }

    @Override
    public String toString() {
        return displayName;
    }
}
