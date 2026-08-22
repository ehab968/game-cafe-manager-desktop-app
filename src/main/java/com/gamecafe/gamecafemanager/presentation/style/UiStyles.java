package com.gamecafe.gamecafemanager.presentation.style;

import java.net.URL;
import java.util.Objects;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

/**
 * Applies the shared presentation stylesheet to JavaFX scenes and dialogs.
 */
public final class UiStyles {

    private static final String STYLESHEET =
            "/com/gamecafe/gamecafemanager/presentation/styles/application.css";

    private UiStyles() {
    }

    public static void apply(Scene scene) {
        Objects.requireNonNull(scene, "scene");
        addIfMissing(scene.getStylesheets());
    }

    public static void apply(DialogPane dialogPane) {
        Objects.requireNonNull(dialogPane, "dialogPane");
        addIfMissing(dialogPane.getStylesheets());
    }

    public static void apply(Parent root) {
        Objects.requireNonNull(root, "root");
        addIfMissing(root.getStylesheets());
    }

    public static String stylesheetUrl() {
        URL resource = UiStyles.class.getResource(STYLESHEET);
        if (resource == null) {
            throw new IllegalStateException("UI stylesheet is missing: " + STYLESHEET);
        }
        return resource.toExternalForm();
    }

    private static void addIfMissing(java.util.List<String> stylesheets) {
        String stylesheet = stylesheetUrl();
        if (!stylesheets.contains(stylesheet)) {
            stylesheets.add(stylesheet);
        }
    }
}
