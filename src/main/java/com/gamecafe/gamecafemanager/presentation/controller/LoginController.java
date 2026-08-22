package com.gamecafe.gamecafemanager.presentation.controller;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.usecase.auth.HasUsersUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.auth.InitializeAdminUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.auth.LoginUseCase;
import com.gamecafe.gamecafemanager.presentation.error.ApplicationErrorHandler;
import com.gamecafe.gamecafemanager.presentation.format.ApplicationDisplayService;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public final class LoginController {

    private final HasUsersUseCase hasUsersUseCase;
    private final InitializeAdminUseCase initializeAdminUseCase;
    private final LoginUseCase loginUseCase;
    private final Consumer<User> onAuthenticated;
    private final ApplicationDisplayService displayService;
    private final ApplicationErrorHandler errorHandler;

    @FXML
    private Label brandLabel;

    @FXML
    private Label titleLabel;

    @FXML
    private Label instructionsLabel;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label confirmPasswordLabel;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button submitButton;

    private boolean setupMode;

    public LoginController(
            HasUsersUseCase hasUsersUseCase,
            InitializeAdminUseCase initializeAdminUseCase,
            LoginUseCase loginUseCase,
            ApplicationDisplayService displayService,
            ApplicationErrorHandler errorHandler,
            Consumer<User> onAuthenticated) {
        this.hasUsersUseCase = Objects.requireNonNull(hasUsersUseCase, "hasUsersUseCase");
        this.initializeAdminUseCase = Objects.requireNonNull(
                initializeAdminUseCase, "initializeAdminUseCase");
        this.loginUseCase = Objects.requireNonNull(loginUseCase, "loginUseCase");
        this.displayService = Objects.requireNonNull(displayService, "displayService");
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
        this.onAuthenticated = Objects.requireNonNull(onAuthenticated, "onAuthenticated");
    }

    @FXML
    private void initialize() {
        setupMode = !hasUsersUseCase.execute();
        brandLabel.setText(displayService.getCafeName().toUpperCase(java.util.Locale.getDefault()));
        titleLabel.setText(setupMode ? "Create administrator" : "Sign in");
        instructionsLabel.setText(setupMode
                ? "Create the first local administrator account."
                : "Sign in to " + displayService.getCafeName() + ".");
        confirmPasswordLabel.setVisible(setupMode);
        confirmPasswordLabel.setManaged(setupMode);
        confirmPasswordField.setVisible(setupMode);
        confirmPasswordField.setManaged(setupMode);
        submitButton.setText(setupMode ? "Create administrator" : "Sign in");
    }

    @FXML
    private void submit() {
        errorLabel.setText("");
        char[] password = passwordField.getText().toCharArray();
        char[] confirmation = confirmPasswordField.getText().toCharArray();
        try {
            if (setupMode) {
                if (!Arrays.equals(password, confirmation)) {
                    throw ValidationException.forField(
                            "password", "Password confirmation does not match");
                }
                initializeAdminUseCase.execute(usernameField.getText(), password);
            }
            User user = loginUseCase.execute(usernameField.getText(), password);
            onAuthenticated.accept(user);
        } catch (RuntimeException exception) {
            errorLabel.setText(errorHandler.handle(
                    exception, "Authentication could not be completed").getMessage());
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
            passwordField.clear();
            confirmPasswordField.clear();
        }
    }
}
