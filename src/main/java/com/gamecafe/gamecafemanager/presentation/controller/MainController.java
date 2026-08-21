package com.gamecafe.gamecafemanager.presentation.controller;

import com.gamecafe.gamecafemanager.core.navigation.NavigationTarget;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controls the application shell and its foundation-level navigation.
 */
public class MainController {

    @FXML
    private Label pageTitle;

    @FXML
    private Label pageDescription;

    @FXML
    private void initialize() {
        navigateTo(NavigationTarget.WELCOME);
    }

    @FXML
    private void showWelcome() {
        navigateTo(NavigationTarget.WELCOME);
    }

    @FXML
    private void showAbout() {
        navigateTo(NavigationTarget.ABOUT);
    }

    private void navigateTo(NavigationTarget target) {
        pageTitle.setText(target.getTitle());
        pageDescription.setText(target.getDescription());
    }
}
