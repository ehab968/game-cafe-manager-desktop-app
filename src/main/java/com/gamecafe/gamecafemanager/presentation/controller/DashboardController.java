package com.gamecafe.gamecafemanager.presentation.controller;

import com.gamecafe.gamecafemanager.core.validation.ValidationException;
import com.gamecafe.gamecafemanager.domain.model.Product;
import com.gamecafe.gamecafemanager.domain.model.CheckoutSummary;
import com.gamecafe.gamecafemanager.domain.model.Invoice;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionMode;
import com.gamecafe.gamecafemanager.domain.model.SessionProduct;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.domain.usecase.session.FinishSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.GetActiveSessionsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.PrepareCheckoutUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.StartSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.GetProductsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.sessionproduct.AddProductToSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.invoice.GenerateInvoiceUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.GetStationsUseCase;
import com.gamecafe.gamecafemanager.presentation.component.UiComponents;
import com.gamecafe.gamecafemanager.presentation.error.ApplicationErrorHandler;
import com.gamecafe.gamecafemanager.presentation.style.UiStyles;
import com.gamecafe.gamecafemanager.presentation.viewmodel.DashboardStationViewModel;
import com.gamecafe.gamecafemanager.presentation.format.ApplicationDisplayService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * Coordinates dashboard rendering and actions through domain use cases. It does
 * not calculate elapsed time or gaming prices.
 */
public class DashboardController {

    private static final DateTimeFormatter CHECKOUT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private final GetStationsUseCase getStationsUseCase;
    private final GetActiveSessionsUseCase getActiveSessionsUseCase;
    private final StartSessionUseCase startSessionUseCase;
    private final FinishSessionUseCase finishSessionUseCase;
    private final PrepareCheckoutUseCase prepareCheckoutUseCase;
    private final GetProductsUseCase getProductsUseCase;
    private final AddProductToSessionUseCase addProductToSessionUseCase;
    private final GenerateInvoiceUseCase generateInvoiceUseCase;
    private final PricingService pricingService;
    private final ApplicationDisplayService displayService;
    private final ApplicationErrorHandler errorHandler;
    private final Clock clock;
    private final List<DashboardStationViewModel> stationCards = new ArrayList<>();

    @FXML
    private TilePane stationCardsPane;

    private Timeline refreshTimeline;

    public DashboardController(
            GetStationsUseCase getStationsUseCase,
            GetActiveSessionsUseCase getActiveSessionsUseCase,
            StartSessionUseCase startSessionUseCase,
            FinishSessionUseCase finishSessionUseCase,
            PrepareCheckoutUseCase prepareCheckoutUseCase,
            GetProductsUseCase getProductsUseCase,
            AddProductToSessionUseCase addProductToSessionUseCase,
            GenerateInvoiceUseCase generateInvoiceUseCase,
            PricingService pricingService,
            ApplicationDisplayService displayService,
            ApplicationErrorHandler errorHandler,
            Clock clock) {
        this.getStationsUseCase = Objects.requireNonNull(getStationsUseCase, "getStationsUseCase");
        this.getActiveSessionsUseCase = Objects.requireNonNull(
                getActiveSessionsUseCase, "getActiveSessionsUseCase");
        this.startSessionUseCase = Objects.requireNonNull(startSessionUseCase, "startSessionUseCase");
        this.finishSessionUseCase = Objects.requireNonNull(
                finishSessionUseCase, "finishSessionUseCase");
        this.prepareCheckoutUseCase = Objects.requireNonNull(
                prepareCheckoutUseCase, "prepareCheckoutUseCase");
        this.getProductsUseCase = Objects.requireNonNull(
                getProductsUseCase, "getProductsUseCase");
        this.addProductToSessionUseCase = Objects.requireNonNull(
                addProductToSessionUseCase, "addProductToSessionUseCase");
        this.generateInvoiceUseCase = Objects.requireNonNull(
                generateInvoiceUseCase, "generateInvoiceUseCase");
        this.pricingService = Objects.requireNonNull(pricingService, "pricingService");
        this.displayService = Objects.requireNonNull(displayService, "displayService");
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @FXML
    private void initialize() {
        reloadDashboard();
        refreshTimeline = new Timeline(new KeyFrame(
                Duration.seconds(1.0),
                event -> refreshLiveValues()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    @FXML
    public void reloadDashboard() {
        stationCards.clear();
        stationCardsPane.getChildren().setAll(
                dashboardState(UiComponents.loadingState(
                        "Loading stations and active sessions…")));
        try {
            Map<Long, Session> activeSessionsByStation = new HashMap<>();
            for (Session session : getActiveSessionsUseCase.execute()) {
                activeSessionsByStation.put(session.getStationId(), session);
            }

            stationCardsPane.getChildren().clear();
            List<Station> stations = getStationsUseCase.execute();
            for (Station station : stations) {
                DashboardStationViewModel card = new DashboardStationViewModel(
                        station,
                        activeSessionsByStation.get(station.getId()),
                        displayService);
                stationCards.add(card);
                stationCardsPane.getChildren().add(createStationCard(card));
            }
            if (stations.isEmpty()) {
                stationCardsPane.getChildren().add(dashboardState(UiComponents.emptyState(
                        "No stations yet",
                        "Create and enable a station to start operating sessions.")));
            }
            refreshLiveValues();
        } catch (RuntimeException exception) {
            stationCardsPane.getChildren().setAll(dashboardState(UiComponents.errorState(
                    "Dashboard unavailable",
                    "Station and session data could not be loaded.",
                    this::reloadDashboard)));
            showError("Could not load dashboard", exception);
        }
    }

    private Node dashboardState(Node state) {
        if (state instanceof Region) {
            Region region = (Region) state;
            region.setPrefWidth(520.0);
            region.setMinHeight(240.0);
        }
        return state;
    }

    private VBox createStationCard(DashboardStationViewModel card) {
        VBox container = new VBox(10.0);
        container.setPrefWidth(248.0);
        container.setMinHeight(236.0);
        container.getStyleClass().add("station-card");
        if (card.isActive()) {
            container.getStyleClass().add("station-card-active");
        } else if (!card.canStart()) {
            container.getStyleClass().add("station-card-disabled");
        }

        Label name = new Label(card.getStationName());
        name.setWrapText(true);
        name.getStyleClass().add("station-name");
        Label type = new Label(card.getStationType());
        type.getStyleClass().add("station-type");
        Label status = new Label(card.getStatusText());
        UiComponents.applyStatusStyle(status, card.getStatusText());
        if (card.isActive()) {
            status.getStyleClass().remove("status-positive");
            status.getStyleClass().add("status-running");
        }

        Label elapsed = new Label();
        elapsed.textProperty().bind(card.elapsedTextProperty());
        elapsed.getStyleClass().add("timer-value");
        Label cost = new Label();
        cost.textProperty().bind(card.currentGamingCostProperty());
        cost.getStyleClass().add("money-value");

        HBox actions = new HBox(8.0);
        if (card.canStart()) {
            Button startButton = new Button("Start");
            startButton.getStyleClass().add("primary-button");
            startButton.setOnAction(event -> startSession(card));
            actions.getChildren().add(startButton);
        } else if (card.isActive()) {
            Button finishButton = new Button("Finish");
            finishButton.getStyleClass().add("danger-button");
            finishButton.setOnAction(event -> finishSession(card));
            Button addProductButton = new Button("Add Product");
            addProductButton.setOnAction(event -> showAddProductDialog(card));
            actions.getChildren().addAll(finishButton, addProductButton);
        }

        Label elapsedCaption = new Label("Elapsed");
        elapsedCaption.getStyleClass().add("field-caption");
        Label costCaption = new Label("Current gaming cost");
        costCaption.getStyleClass().add("field-caption");
        container.getChildren().addAll(
                name,
                type,
                status,
                elapsedCaption,
                elapsed,
                costCaption,
                cost,
                actions);
        return container;
    }

    private void startSession(DashboardStationViewModel card) {
        try {
            if (card.supportsSessionModes()) {
                Optional<SessionMode> selectedMode = showSessionModeDialog(card);
                if (!selectedMode.isPresent()) {
                    return;
                }
                startSessionUseCase.execute(card.getStationId(), selectedMode.get());
            } else {
                startSessionUseCase.execute(card.getStationId());
            }
            reloadDashboard();
        } catch (RuntimeException exception) {
            showError("Could not start session", exception);
        }
    }

    private Optional<SessionMode> showSessionModeDialog(DashboardStationViewModel card) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        UiStyles.apply(dialog.getDialogPane());
        dialog.setTitle("Start session");
        dialog.setHeaderText("Choose a mode for " + card.getStationName());
        dialog.setContentText("The selected rate will be captured for this session.");
        dialog.initOwner(stationCardsPane.getScene().getWindow());

        ButtonType singleType = new ButtonType(
                "Single — " + displayService.formatMoney(card.getSingleHourlyRate()),
                ButtonBar.ButtonData.OK_DONE);
        ButtonType multiType = new ButtonType(
                "Multi — " + displayService.formatMoney(card.getMultiHourlyRate()),
                ButtonBar.ButtonData.OTHER);
        dialog.getButtonTypes().setAll(singleType, multiType, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(singleType))
                .getStyleClass().add("primary-button");

        return dialog.showAndWait().flatMap(selected -> {
            if (selected.equals(singleType)) {
                return Optional.of(SessionMode.SINGLE);
            }
            if (selected.equals(multiType)) {
                return Optional.of(SessionMode.MULTI);
            }
            return Optional.empty();
        });
    }

    private void finishSession(DashboardStationViewModel card) {
        try {
            CheckoutSummary checkout = prepareCheckoutUseCase.execute(
                    card.getActiveSessionId());
            if (showCheckoutConfirmation(checkout)) {
                Session completed = finishSessionUseCase.execute(
                        checkout.getSessionId(), checkout.getEndTime());
                Invoice invoice = generateInvoiceUseCase.execute(completed.getId());
                reloadDashboard();
                new InvoicePreviewDialog().show(
                        invoice,
                        stationCardsPane.getScene() == null
                                ? null
                                : stationCardsPane.getScene().getWindow());
            }
        } catch (RuntimeException exception) {
            showError("Could not finish session", exception);
        }
    }

    private boolean showCheckoutConfirmation(CheckoutSummary checkout) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        UiStyles.apply(dialog.getDialogPane());
        dialog.setTitle("Session checkout");
        dialog.setHeaderText("Confirm checkout for " + checkout.getStationName());
        dialog.initOwner(stationCardsPane.getScene().getWindow());
        dialog.setResizable(true);

        GridPane details = new GridPane();
        details.setHgap(18.0);
        details.setVgap(8.0);
        int detailRow = 0;
        details.addRow(detailRow++, new Label("Station"), new Label(checkout.getStationName()));
        if (checkout.getMode() != null) {
            details.addRow(
                    detailRow++,
                    new Label("Mode"),
                    new Label(checkout.getMode().getDisplayName()));
        }
        details.addRow(
                detailRow++,
                new Label("Hourly rate"),
                new Label(displayService.formatMoney(checkout.getHourlyRateSnapshot()) + "/hour"));
        details.addRow(detailRow++, new Label("Start time"),
                new Label(CHECKOUT_TIME_FORMAT.format(checkout.getStartTime())));
        details.addRow(detailRow++, new Label("End time"),
                new Label(CHECKOUT_TIME_FORMAT.format(checkout.getEndTime())));
        details.addRow(detailRow++, new Label("Duration"),
                new Label(formatDuration(checkout.getDuration())));
        details.addRow(detailRow, new Label("Gaming cost"),
                new Label(displayService.formatMoney(checkout.getGamingCost())));

        VBox productLines = new VBox(6.0);
        if (checkout.getPurchasedProducts().isEmpty()) {
            productLines.getChildren().add(new Label("No products purchased"));
        } else {
            for (SessionProduct item : checkout.getPurchasedProducts()) {
                productLines.getChildren().add(new Label(
                        item.getQuantity() + " × " + item.getProductNameSnapshot()
                                + " @ " + displayService.formatMoney(
                                        item.getUnitPriceSnapshot())
                                + " = " + displayService.formatMoney(item.getLineTotal())));
            }
        }
        ScrollPane productsPane = new ScrollPane(productLines);
        productsPane.setFitToWidth(true);
        productsPane.setPrefViewportHeight(Math.min(
                150.0, 34.0 + checkout.getPurchasedProducts().size() * 28.0));

        GridPane totals = new GridPane();
        totals.setHgap(18.0);
        totals.setVgap(8.0);
        totals.addRow(0, new Label("Products total"),
                new Label(displayService.formatMoney(checkout.getProductsTotal())));
        Label finalTotal = new Label(displayService.formatMoney(checkout.getFinalTotal()));
        finalTotal.getStyleClass().add("checkout-total");
        totals.addRow(1, new Label("Final total"), finalTotal);

        VBox content = new VBox(
                12.0,
                details,
                new Separator(),
                new Label("Purchased products"),
                productsPane,
                new Separator(),
                totals);
        content.setPadding(new Insets(4.0));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(560.0);

        ButtonType confirmType = new ButtonType(
                "Confirm checkout", ButtonBar.ButtonData.OK_DONE);
        dialog.getButtonTypes().setAll(confirmType, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(confirmType))
                .getStyleClass().add("primary-button");
        return dialog.showAndWait()
                .filter(confirmType::equals)
                .isPresent();
    }

    private String formatDuration(java.time.Duration duration) {
        long totalSeconds = duration.getSeconds();
        long hours = totalSeconds / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void refreshLiveValues() {
        Instant currentTime = clock.instant();
        for (DashboardStationViewModel card : stationCards) {
            card.refresh(currentTime, pricingService);
        }
    }

    private void showAddProductDialog(DashboardStationViewModel card) {
        List<Product> availableProducts;
        try {
            availableProducts = getProductsUseCase.execute().stream()
                    .filter(Product::isEnabled)
                    .filter(product -> product.getStockQuantity() > 0)
                    .collect(Collectors.toList());
        } catch (RuntimeException exception) {
            showError("Could not load products", exception);
            return;
        }
        if (availableProducts.isEmpty()) {
            showInformation(
                    "No products available",
                    "Enable products and add stock before attaching them to a session.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        UiStyles.apply(dialog.getDialogPane());
        dialog.setTitle("Add product");
        dialog.setHeaderText("Add a product to " + card.getStationName());
        dialog.initOwner(stationCardsPane.getScene().getWindow());

        ComboBox<Product> productField = new ComboBox<>(
                FXCollections.observableArrayList(availableProducts));
        productField.setMaxWidth(Double.MAX_VALUE);
        productField.setConverter(new StringConverter<Product>() {
            @Override
            public String toString(Product product) {
                return product == null
                        ? ""
                        : product.getName() + " — "
                                + displayService.formatMoney(product.getCurrentPrice())
                                + " (stock " + product.getStockQuantity() + ")";
            }

            @Override
            public Product fromString(String value) {
                return null;
            }
        });
        productField.setValue(availableProducts.get(0));
        TextField quantityField = new TextField("1");

        GridPane form = new GridPane();
        form.setHgap(12.0);
        form.setVgap(12.0);
        form.setPadding(new Insets(8.0, 0.0, 0.0, 0.0));
        form.addRow(0, new Label("Product"), productField);
        form.addRow(1, new Label("Quantity"), quantityField);
        dialog.getDialogPane().setContent(form);

        ButtonType addType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);
        Button addButton = (Button) dialog.getDialogPane().lookupButton(addType);
        addButton.getStyleClass().add("primary-button");
        boolean[] added = {false};
        addButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                Product selectedProduct = productField.getValue();
                if (selectedProduct == null) {
                    throw ValidationException.forField("product", "Select a product");
                }
                addProductToSessionUseCase.execute(
                        card.getActiveSessionId(),
                        selectedProduct.getId(),
                        parseQuantity(quantityField.getText()));
                added[0] = true;
            } catch (ValidationException exception) {
                showError("Check product details", exception);
                event.consume();
            } catch (RuntimeException exception) {
                showError("Could not add product", exception);
                event.consume();
            }
        });
        dialog.showAndWait();
        if (added[0]) {
            reloadDashboard();
        }
    }

    private int parseQuantity(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException exception) {
            throw ValidationException.forField("quantity", "Enter a whole quantity");
        }
    }

    private void showInformation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        UiStyles.apply(alert.getDialogPane());
        alert.setTitle(displayService.getCafeName());
        alert.setHeaderText(title);
        alert.setContentText(message);
        if (stationCardsPane.getScene() != null) {
            alert.initOwner(stationCardsPane.getScene().getWindow());
        }
        alert.showAndWait();
    }

    private void showError(String title, Throwable failure) {
        errorHandler.show(
                stationCardsPane.getScene() == null
                        ? null
                        : stationCardsPane.getScene().getWindow(),
                title,
                failure);
    }

    public void dispose() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }
}
