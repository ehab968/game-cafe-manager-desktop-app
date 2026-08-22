package com.gamecafe.gamecafemanager.presentation.controller;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.Product;
import com.gamecafe.gamecafemanager.domain.usecase.product.CreateProductUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.GetProductsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.SetProductEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.UpdateProductStockUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.UpdateProductUseCase;
import com.gamecafe.gamecafemanager.presentation.component.UiComponents;
import com.gamecafe.gamecafemanager.presentation.error.ApplicationErrorHandler;
import com.gamecafe.gamecafemanager.presentation.format.ApplicationDisplayService;
import com.gamecafe.gamecafemanager.presentation.style.UiStyles;
import java.math.BigDecimal;
import java.util.Objects;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * Presents product inventory while delegating validation and persistence to use cases.
 */
public final class ProductsController {

    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final GetProductsUseCase getProductsUseCase;
    private final SetProductEnabledUseCase setProductEnabledUseCase;
    private final UpdateProductStockUseCase updateProductStockUseCase;
    private final ApplicationDisplayService displayService;
    private final ApplicationErrorHandler errorHandler;
    private final ObservableList<Product> products = FXCollections.observableArrayList();

    @FXML
    private TableView<Product> productTable;

    @FXML
    private TableColumn<Product, String> nameColumn;

    @FXML
    private TableColumn<Product, String> priceColumn;

    @FXML
    private TableColumn<Product, Integer> stockColumn;

    @FXML
    private TableColumn<Product, String> statusColumn;

    @FXML
    private Button editButton;

    @FXML
    private Button updateStockButton;

    @FXML
    private Button toggleEnabledButton;

    public ProductsController(
            CreateProductUseCase createProductUseCase,
            UpdateProductUseCase updateProductUseCase,
            GetProductsUseCase getProductsUseCase,
            SetProductEnabledUseCase setProductEnabledUseCase,
            UpdateProductStockUseCase updateProductStockUseCase,
            ApplicationDisplayService displayService,
            ApplicationErrorHandler errorHandler) {
        this.createProductUseCase = Objects.requireNonNull(
                createProductUseCase, "createProductUseCase");
        this.updateProductUseCase = Objects.requireNonNull(
                updateProductUseCase, "updateProductUseCase");
        this.getProductsUseCase = Objects.requireNonNull(
                getProductsUseCase, "getProductsUseCase");
        this.setProductEnabledUseCase = Objects.requireNonNull(
                setProductEnabledUseCase, "setProductEnabledUseCase");
        this.updateProductStockUseCase = Objects.requireNonNull(
                updateProductStockUseCase, "updateProductStockUseCase");
        this.displayService = Objects.requireNonNull(displayService, "displayService");
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
    }

    @FXML
    private void initialize() {
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
        priceColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                displayService.formatMoney(cell.getValue().getCurrentPrice())));
        stockColumn.setCellValueFactory(
                cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getStockQuantity()));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().isEnabled() ? "Enabled" : "Disabled"));
        statusColumn.setCellFactory(UiComponents.statusCellFactory());
        productTable.setItems(products);
        productTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> updateActionState(selected));
        updateActionState(null);
        refresh();
    }

    @FXML
    private void createProduct() {
        showProductDialog(null);
    }

    @FXML
    private void editProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showProductDialog(selected);
        }
    }

    @FXML
    private void updateStock() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        Dialog<Void> dialog = createDialog(
                "Update stock", "Set stock for " + selected.getName());
        TextField stockField = new TextField(Integer.toString(selected.getStockQuantity()));
        stockField.setPromptText("0");
        GridPane form = createForm();
        form.addRow(0, new Label("Stock quantity"), stockField);
        dialog.getDialogPane().setContent(form);
        ButtonType saveType = addSaveAndCancelButtons(dialog, "Update");
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.getStyleClass().add("primary-button");
        boolean[] saved = {false};
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                updateProductStockUseCase.execute(
                        selected.getId(), parseStockQuantity(stockField.getText()));
                saved[0] = true;
            } catch (ValidationException exception) {
                showValidationErrors(exception);
                event.consume();
            } catch (RuntimeException exception) {
                showError("Could not update stock", exception);
                event.consume();
            }
        });
        dialog.showAndWait();
        if (saved[0]) {
            refresh();
        }
    }

    @FXML
    private void toggleProductEnabled() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            setProductEnabledUseCase.execute(selected.getId(), !selected.isEnabled());
            refresh();
        } catch (RuntimeException exception) {
            showError("Could not change product status", exception);
        }
    }

    public void refresh() {
        products.clear();
        productTable.setPlaceholder(UiComponents.loadingState("Loading products…"));
        try {
            products.setAll(getProductsUseCase.execute());
            productTable.setPlaceholder(UiComponents.emptyState(
                    "No products yet",
                    "Create a product to begin tracking price and stock."));
            productTable.refresh();
            updateActionState(productTable.getSelectionModel().getSelectedItem());
        } catch (RuntimeException exception) {
            productTable.setPlaceholder(UiComponents.errorState(
                    "Products unavailable",
                    "Product and inventory data could not be loaded.",
                    this::refresh));
            showError("Could not load products", exception);
        }
    }

    private void showProductDialog(Product existing) {
        boolean creating = existing == null;
        Dialog<Void> dialog = createDialog(
                creating ? "Create product" : "Edit product",
                creating ? "Add an inventory product" : "Update " + existing.getName());
        TextField nameField = new TextField(creating ? "" : existing.getName());
        TextField priceField = new TextField(
                creating ? "" : existing.getCurrentPrice().toPlainString());
        priceField.setPromptText("0.00");
        TextField stockField = new TextField(
                creating ? "0" : Integer.toString(existing.getStockQuantity()));
        stockField.setPromptText("0");

        GridPane form = createForm();
        form.addRow(0, new Label("Name"), nameField);
        form.addRow(1, new Label("Current price"), priceField);
        form.addRow(2, new Label("Stock quantity"), stockField);
        dialog.getDialogPane().setContent(form);
        ButtonType saveType = addSaveAndCancelButtons(dialog, creating ? "Create" : "Save");
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.getStyleClass().add("primary-button");
        boolean[] saved = {false};
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                BigDecimal price = parseCurrentPrice(priceField.getText());
                int stockQuantity = parseStockQuantity(stockField.getText());
                if (creating) {
                    createProductUseCase.execute(nameField.getText(), price, stockQuantity);
                } else {
                    updateProductUseCase.execute(
                            existing.getId(), nameField.getText(), price, stockQuantity);
                }
                saved[0] = true;
            } catch (ValidationException exception) {
                showValidationErrors(exception);
                event.consume();
            } catch (RuntimeException exception) {
                showError("Could not save product", exception);
                event.consume();
            }
        });
        dialog.showAndWait();
        if (saved[0]) {
            refresh();
        }
    }

    private Dialog<Void> createDialog(String title, String header) {
        Dialog<Void> dialog = new Dialog<>();
        UiStyles.apply(dialog.getDialogPane());
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.initOwner(productTable.getScene().getWindow());
        return dialog;
    }

    private GridPane createForm() {
        GridPane form = new GridPane();
        form.setHgap(12.0);
        form.setVgap(12.0);
        form.setPadding(new Insets(8.0, 0.0, 0.0, 0.0));
        return form;
    }

    private ButtonType addSaveAndCancelButtons(Dialog<Void> dialog, String saveText) {
        ButtonType saveType = new ButtonType(saveText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        return saveType;
    }

    private BigDecimal parseCurrentPrice(String value) {
        try {
            return new BigDecimal(value == null ? "" : value.trim());
        } catch (NumberFormatException exception) {
            throw ValidationException.forField("currentPrice", "Enter a valid current price");
        }
    }

    private int parseStockQuantity(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException exception) {
            throw ValidationException.forField("stockQuantity", "Enter a whole stock quantity");
        }
    }

    private void updateActionState(Product product) {
        boolean noSelection = product == null;
        editButton.setDisable(noSelection);
        updateStockButton.setDisable(noSelection);
        toggleEnabledButton.setDisable(noSelection);
        toggleEnabledButton.setText(noSelection || product.isEnabled() ? "Disable" : "Enable");
        toggleEnabledButton.getStyleClass().remove("danger-button");
        if (!noSelection && product.isEnabled()) {
            toggleEnabledButton.getStyleClass().add("danger-button");
        }
    }

    private void showValidationErrors(ValidationException exception) {
        showError("Check product details", exception);
    }

    private void showError(String title, Throwable failure) {
        errorHandler.show(
                productTable.getScene() == null
                        ? null
                        : productTable.getScene().getWindow(),
                title,
                failure);
    }
}
