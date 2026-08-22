package com.gamecafe.gamecafemanager.presentation.controller;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.Role;
import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.usecase.user.CreateUserUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.GetUsersUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.ResetUserPasswordUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.SetUserEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.UpdateUserRoleUseCase;
import com.gamecafe.gamecafemanager.presentation.component.UiComponents;
import com.gamecafe.gamecafemanager.presentation.error.ApplicationErrorHandler;
import com.gamecafe.gamecafemanager.presentation.style.UiStyles;
import java.util.Arrays;
import java.util.Objects;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public final class UsersController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUsersUseCase getUsersUseCase;
    private final UpdateUserRoleUseCase updateUserRoleUseCase;
    private final SetUserEnabledUseCase setUserEnabledUseCase;
    private final ResetUserPasswordUseCase resetUserPasswordUseCase;
    private final ApplicationErrorHandler errorHandler;
    private final ObservableList<User> users = FXCollections.observableArrayList();

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private Button roleButton;
    @FXML private Button passwordButton;
    @FXML private Button toggleEnabledButton;

    public UsersController(
            CreateUserUseCase createUserUseCase,
            GetUsersUseCase getUsersUseCase,
            UpdateUserRoleUseCase updateUserRoleUseCase,
            SetUserEnabledUseCase setUserEnabledUseCase,
            ResetUserPasswordUseCase resetUserPasswordUseCase,
            ApplicationErrorHandler errorHandler) {
        this.createUserUseCase = Objects.requireNonNull(createUserUseCase, "createUserUseCase");
        this.getUsersUseCase = Objects.requireNonNull(getUsersUseCase, "getUsersUseCase");
        this.updateUserRoleUseCase = Objects.requireNonNull(
                updateUserRoleUseCase, "updateUserRoleUseCase");
        this.setUserEnabledUseCase = Objects.requireNonNull(
                setUserEnabledUseCase, "setUserEnabledUseCase");
        this.resetUserPasswordUseCase = Objects.requireNonNull(
                resetUserPasswordUseCase, "resetUserPasswordUseCase");
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
    }

    @FXML
    private void initialize() {
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        usernameColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getUsername()));
        roleColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getRole().name()));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().isEnabled() ? "Enabled" : "Disabled"));
        statusColumn.setCellFactory(UiComponents.statusCellFactory());
        userTable.setItems(users);
        userTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> updateActionState(selected));
        updateActionState(null);
        refresh();
    }

    @FXML
    private void createUser() {
        Dialog<Void> dialog = createDialog("Create user", "Add a local user account");
        TextField username = new TextField();
        ComboBox<Role> role = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        role.setValue(Role.CASHIER);
        role.setMaxWidth(Double.MAX_VALUE);
        PasswordField password = new PasswordField();
        PasswordField confirmation = new PasswordField();
        GridPane form = createForm();
        form.addRow(0, new Label("Username"), username);
        form.addRow(1, new Label("Role"), role);
        form.addRow(2, new Label("Password"), password);
        form.addRow(3, new Label("Confirm password"), confirmation);
        dialog.getDialogPane().setContent(form);
        ButtonType saveType = addButtons(dialog, "Create");
        boolean[] saved = {false};
        ((Button) dialog.getDialogPane().lookupButton(saveType)).addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    char[] value = password.getText().toCharArray();
                    char[] repeated = confirmation.getText().toCharArray();
                    try {
                        requireMatchingPasswords(value, repeated);
                        createUserUseCase.execute(username.getText(), role.getValue(), value);
                        saved[0] = true;
                    } catch (ValidationException exception) {
                        showValidationErrors(exception);
                        event.consume();
                    } catch (RuntimeException exception) {
                        showError("Could not create user", exception);
                        event.consume();
                    } finally {
                        Arrays.fill(value, '\0');
                        Arrays.fill(repeated, '\0');
                    }
                });
        dialog.showAndWait();
        if (saved[0]) refresh();
    }

    @FXML
    private void changeRole() {
        User selected = selectedUser();
        if (selected == null) return;
        Dialog<Void> dialog = createDialog("Change role", "Update " + selected.getUsername());
        ComboBox<Role> role = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        role.setValue(selected.getRole());
        role.setMaxWidth(Double.MAX_VALUE);
        GridPane form = createForm();
        form.addRow(0, new Label("Role"), role);
        dialog.getDialogPane().setContent(form);
        ButtonType saveType = addButtons(dialog, "Save");
        boolean[] saved = {false};
        ((Button) dialog.getDialogPane().lookupButton(saveType)).addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    try {
                        updateUserRoleUseCase.execute(selected.getId(), role.getValue());
                        saved[0] = true;
                    } catch (ValidationException exception) {
                        showValidationErrors(exception);
                        event.consume();
                    } catch (RuntimeException exception) {
                        showError("Could not change role", exception);
                        event.consume();
                    }
                });
        dialog.showAndWait();
        if (saved[0]) refresh();
    }

    @FXML
    private void resetPassword() {
        User selected = selectedUser();
        if (selected == null) return;
        Dialog<Void> dialog = createDialog(
                "Reset password", "Set a new password for " + selected.getUsername());
        PasswordField password = new PasswordField();
        PasswordField confirmation = new PasswordField();
        GridPane form = createForm();
        form.addRow(0, new Label("New password"), password);
        form.addRow(1, new Label("Confirm password"), confirmation);
        dialog.getDialogPane().setContent(form);
        ButtonType saveType = addButtons(dialog, "Reset");
        boolean[] saved = {false};
        ((Button) dialog.getDialogPane().lookupButton(saveType)).addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    char[] value = password.getText().toCharArray();
                    char[] repeated = confirmation.getText().toCharArray();
                    try {
                        requireMatchingPasswords(value, repeated);
                        resetUserPasswordUseCase.execute(selected.getId(), value);
                        saved[0] = true;
                    } catch (ValidationException exception) {
                        showValidationErrors(exception);
                        event.consume();
                    } catch (RuntimeException exception) {
                        showError("Could not reset password", exception);
                        event.consume();
                    } finally {
                        Arrays.fill(value, '\0');
                        Arrays.fill(repeated, '\0');
                    }
                });
        dialog.showAndWait();
        if (saved[0]) refresh();
    }

    @FXML
    private void toggleEnabled() {
        User selected = selectedUser();
        if (selected == null) return;
        try {
            setUserEnabledUseCase.execute(selected.getId(), !selected.isEnabled());
            refresh();
        } catch (ValidationException exception) {
            showValidationErrors(exception);
        } catch (RuntimeException exception) {
            showError("Could not change user status", exception);
        }
    }

    public void refresh() {
        users.clear();
        userTable.setPlaceholder(UiComponents.loadingState("Loading users…"));
        try {
            users.setAll(getUsersUseCase.execute());
            userTable.setPlaceholder(UiComponents.emptyState(
                    "No users found",
                    "Create an account to grant access to the application."));
            userTable.refresh();
            updateActionState(selectedUser());
        } catch (RuntimeException exception) {
            userTable.setPlaceholder(UiComponents.errorState(
                    "Users unavailable",
                    "User accounts could not be loaded.",
                    this::refresh));
            showError("Could not load users", exception);
        }
    }

    private User selectedUser() {
        return userTable.getSelectionModel().getSelectedItem();
    }

    private void updateActionState(User user) {
        boolean none = user == null;
        roleButton.setDisable(none);
        passwordButton.setDisable(none);
        toggleEnabledButton.setDisable(none);
        toggleEnabledButton.setText(none || user.isEnabled() ? "Disable" : "Enable");
        toggleEnabledButton.getStyleClass().remove("danger-button");
        if (!none && user.isEnabled()) {
            toggleEnabledButton.getStyleClass().add("danger-button");
        }
    }

    private void requireMatchingPasswords(char[] password, char[] confirmation) {
        if (!Arrays.equals(password, confirmation)) {
            throw ValidationException.forField("password", "Password confirmation does not match");
        }
    }

    private Dialog<Void> createDialog(String title, String header) {
        Dialog<Void> dialog = new Dialog<>();
        UiStyles.apply(dialog.getDialogPane());
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.initOwner(userTable.getScene().getWindow());
        return dialog;
    }

    private GridPane createForm() {
        GridPane form = new GridPane();
        form.setHgap(12.0);
        form.setVgap(12.0);
        form.setPadding(new Insets(8.0, 0.0, 0.0, 0.0));
        return form;
    }

    private ButtonType addButtons(Dialog<Void> dialog, String text) {
        ButtonType saveType = new ButtonType(text, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(saveType).getStyleClass().add("primary-button");
        return saveType;
    }

    private void showValidationErrors(ValidationException exception) {
        showError("Check user details", exception);
    }

    private void showError(String title, Throwable failure) {
        errorHandler.show(
                userTable.getScene() == null ? null : userTable.getScene().getWindow(),
                title,
                failure);
    }
}
