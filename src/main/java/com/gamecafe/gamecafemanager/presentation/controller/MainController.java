package com.gamecafe.gamecafemanager.presentation.controller;

import com.gamecafe.gamecafemanager.core.navigation.NavigationTarget;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.domain.usecase.product.CreateProductUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.GetProductsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.SetProductEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.UpdateProductStockUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.UpdateProductUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.invoice.GenerateInvoiceUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.report.GetReportUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.settings.GetSettingsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.settings.UpdateSettingsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.auth.LogoutUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.CreateUserUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.GetUsersUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.ResetUserPasswordUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.SetUserEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.UpdateUserRoleUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.FinishSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.GetActiveSessionsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.PrepareCheckoutUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.StartSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.sessionproduct.AddProductToSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.CreateStationUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.GetStationsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.SetStationEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.UpdateStationUseCase;
import com.gamecafe.gamecafemanager.presentation.error.ApplicationErrorHandler;
import com.gamecafe.gamecafemanager.presentation.format.ApplicationDisplayService;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controls the application shell and its foundation-level navigation.
 */
public class MainController {

    private static final String DASHBOARD_VIEW =
            "/com/gamecafe/gamecafemanager/presentation/view/dashboard-view.fxml";
    private static final String STATIONS_VIEW =
            "/com/gamecafe/gamecafemanager/presentation/view/stations-view.fxml";
    private static final String PRODUCTS_VIEW =
            "/com/gamecafe/gamecafemanager/presentation/view/products-view.fxml";
    private static final String USERS_VIEW =
            "/com/gamecafe/gamecafemanager/presentation/view/users-view.fxml";
    private static final String REPORTS_VIEW =
            "/com/gamecafe/gamecafemanager/presentation/view/reports-view.fxml";
    private static final String SETTINGS_VIEW =
            "/com/gamecafe/gamecafemanager/presentation/view/settings-view.fxml";
    private static final String ACTIVE_SESSIONS_VIEW =
            "/com/gamecafe/gamecafemanager/presentation/view/active-sessions-view.fxml";

    private final CreateStationUseCase createStationUseCase;
    private final UpdateStationUseCase updateStationUseCase;
    private final GetStationsUseCase getStationsUseCase;
    private final SetStationEnabledUseCase setStationEnabledUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final GetProductsUseCase getProductsUseCase;
    private final SetProductEnabledUseCase setProductEnabledUseCase;
    private final UpdateProductStockUseCase updateProductStockUseCase;
    private final GetActiveSessionsUseCase getActiveSessionsUseCase;
    private final StartSessionUseCase startSessionUseCase;
    private final FinishSessionUseCase finishSessionUseCase;
    private final PrepareCheckoutUseCase prepareCheckoutUseCase;
    private final AddProductToSessionUseCase addProductToSessionUseCase;
    private final GenerateInvoiceUseCase generateInvoiceUseCase;
    private final GetReportUseCase getReportUseCase;
    private final GetSettingsUseCase getSettingsUseCase;
    private final UpdateSettingsUseCase updateSettingsUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final GetUsersUseCase getUsersUseCase;
    private final UpdateUserRoleUseCase updateUserRoleUseCase;
    private final SetUserEnabledUseCase setUserEnabledUseCase;
    private final ResetUserPasswordUseCase resetUserPasswordUseCase;
    private final AuthorizationService authorization;
    private final LogoutUseCase logoutUseCase;
    private final User authenticatedUser;
    private final Runnable onLogout;
    private final PricingService pricingService;
    private final ApplicationDisplayService displayService;
    private final ApplicationErrorHandler errorHandler;
    private final Clock clock;
    private final Runnable onSettingsChanged;

    @FXML
    private VBox informationPage;

    @FXML
    private StackPane contentArea;

    @FXML
    private Label pageTitle;

    @FXML
    private Label pageDescription;

    @FXML private Button dashboardNavigationButton;
    @FXML private Button welcomeNavigationButton;
    @FXML private Button stationsNavigationButton;
    @FXML private Button productsNavigationButton;
    @FXML private Button usersNavigationButton;
    @FXML private Button reportsNavigationButton;
    @FXML private Button settingsNavigationButton;
    @FXML private Button activeSessionsNavigationButton;
    @FXML private Button aboutNavigationButton;
    @FXML private Label cafeNameLabel;
    @FXML private Label currentUserLabel;
    @FXML private Label currentRoleLabel;

    private Parent stationsView;
    private StationsController stationsController;
    private Parent productsView;
    private ProductsController productsController;
    private Parent usersView;
    private UsersController usersController;
    private Parent reportsView;
    private ReportsController reportsController;
    private Parent settingsView;
    private SettingsController settingsController;
    private Parent activeSessionsView;
    private ActiveSessionsController activeSessionsController;
    private Parent dashboardView;
    private DashboardController dashboardController;

    public MainController(
            CreateStationUseCase createStationUseCase,
            UpdateStationUseCase updateStationUseCase,
            GetStationsUseCase getStationsUseCase,
            SetStationEnabledUseCase setStationEnabledUseCase,
            CreateProductUseCase createProductUseCase,
            UpdateProductUseCase updateProductUseCase,
            GetProductsUseCase getProductsUseCase,
            SetProductEnabledUseCase setProductEnabledUseCase,
            UpdateProductStockUseCase updateProductStockUseCase,
            GetActiveSessionsUseCase getActiveSessionsUseCase,
            StartSessionUseCase startSessionUseCase,
            FinishSessionUseCase finishSessionUseCase,
            PrepareCheckoutUseCase prepareCheckoutUseCase,
            AddProductToSessionUseCase addProductToSessionUseCase,
            GenerateInvoiceUseCase generateInvoiceUseCase,
            GetReportUseCase getReportUseCase,
            GetSettingsUseCase getSettingsUseCase,
            UpdateSettingsUseCase updateSettingsUseCase,
            CreateUserUseCase createUserUseCase,
            GetUsersUseCase getUsersUseCase,
            UpdateUserRoleUseCase updateUserRoleUseCase,
            SetUserEnabledUseCase setUserEnabledUseCase,
            ResetUserPasswordUseCase resetUserPasswordUseCase,
            PricingService pricingService,
            ApplicationDisplayService displayService,
            ApplicationErrorHandler errorHandler,
            Clock clock,
            AuthorizationService authorization,
            LogoutUseCase logoutUseCase,
            User authenticatedUser,
            Runnable onSettingsChanged,
            Runnable onLogout) {
        this.createStationUseCase = Objects.requireNonNull(createStationUseCase, "createStationUseCase");
        this.updateStationUseCase = Objects.requireNonNull(updateStationUseCase, "updateStationUseCase");
        this.getStationsUseCase = Objects.requireNonNull(getStationsUseCase, "getStationsUseCase");
        this.setStationEnabledUseCase = Objects.requireNonNull(
                setStationEnabledUseCase, "setStationEnabledUseCase");
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
        this.getActiveSessionsUseCase = Objects.requireNonNull(
                getActiveSessionsUseCase, "getActiveSessionsUseCase");
        this.startSessionUseCase = Objects.requireNonNull(startSessionUseCase, "startSessionUseCase");
        this.finishSessionUseCase = Objects.requireNonNull(
                finishSessionUseCase, "finishSessionUseCase");
        this.prepareCheckoutUseCase = Objects.requireNonNull(
                prepareCheckoutUseCase, "prepareCheckoutUseCase");
        this.addProductToSessionUseCase = Objects.requireNonNull(
                addProductToSessionUseCase, "addProductToSessionUseCase");
        this.generateInvoiceUseCase = Objects.requireNonNull(
                generateInvoiceUseCase, "generateInvoiceUseCase");
        this.getReportUseCase = Objects.requireNonNull(getReportUseCase, "getReportUseCase");
        this.getSettingsUseCase = Objects.requireNonNull(getSettingsUseCase, "getSettingsUseCase");
        this.updateSettingsUseCase = Objects.requireNonNull(
                updateSettingsUseCase, "updateSettingsUseCase");
        this.createUserUseCase = Objects.requireNonNull(createUserUseCase, "createUserUseCase");
        this.getUsersUseCase = Objects.requireNonNull(getUsersUseCase, "getUsersUseCase");
        this.updateUserRoleUseCase = Objects.requireNonNull(
                updateUserRoleUseCase, "updateUserRoleUseCase");
        this.setUserEnabledUseCase = Objects.requireNonNull(
                setUserEnabledUseCase, "setUserEnabledUseCase");
        this.resetUserPasswordUseCase = Objects.requireNonNull(
                resetUserPasswordUseCase, "resetUserPasswordUseCase");
        this.pricingService = Objects.requireNonNull(pricingService, "pricingService");
        this.displayService = Objects.requireNonNull(displayService, "displayService");
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
        this.logoutUseCase = Objects.requireNonNull(logoutUseCase, "logoutUseCase");
        this.authenticatedUser = Objects.requireNonNull(authenticatedUser, "authenticatedUser");
        this.onSettingsChanged = Objects.requireNonNull(
                onSettingsChanged, "onSettingsChanged");
        this.onLogout = Objects.requireNonNull(onLogout, "onLogout");
    }

    @FXML
    private void initialize() {
        refreshBranding();
        currentUserLabel.setText(authenticatedUser.getUsername());
        currentRoleLabel.setText(authenticatedUser.getRole().name());
        configureNavigation();
        navigateTo(NavigationTarget.DASHBOARD);
    }

    private void configureNavigation() {
        setNavigationAllowed(
                stationsNavigationButton,
                authorization.isAllowed(Permission.MANAGE_STATIONS));
        setNavigationAllowed(
                productsNavigationButton,
                authorization.isAllowed(Permission.MANAGE_PRODUCTS));
        setNavigationAllowed(
                usersNavigationButton,
                authorization.isAllowed(Permission.MANAGE_USERS));
        setNavigationAllowed(
                reportsNavigationButton,
                authorization.isAllowed(Permission.VIEW_REPORTS));
        setNavigationAllowed(
                settingsNavigationButton,
                authorization.isAllowed(Permission.MANAGE_SETTINGS));
    }

    private void setNavigationAllowed(Button button, boolean allowed) {
        button.setVisible(allowed);
        button.setManaged(allowed);
    }

    @FXML
    private void showDashboard() {
        navigateTo(NavigationTarget.DASHBOARD);
    }

    @FXML
    private void showWelcome() {
        navigateTo(NavigationTarget.WELCOME);
    }

    @FXML
    private void showStations() {
        authorization.require(Permission.MANAGE_STATIONS);
        navigateTo(NavigationTarget.STATIONS);
    }

    @FXML
    private void showProducts() {
        authorization.require(Permission.MANAGE_PRODUCTS);
        navigateTo(NavigationTarget.PRODUCTS);
    }

    @FXML
    private void showUsers() {
        authorization.require(Permission.MANAGE_USERS);
        navigateTo(NavigationTarget.USERS);
    }

    @FXML
    private void showReports() {
        authorization.require(Permission.VIEW_REPORTS);
        navigateTo(NavigationTarget.REPORTS);
    }

    @FXML
    private void showSettings() {
        authorization.require(Permission.MANAGE_SETTINGS);
        navigateTo(NavigationTarget.SETTINGS);
    }

    @FXML
    private void logout() {
        dispose();
        logoutUseCase.execute();
        onLogout.run();
    }

    @FXML
    private void showActiveSessions() {
        navigateTo(NavigationTarget.ACTIVE_SESSIONS);
    }

    @FXML
    private void showAbout() {
        navigateTo(NavigationTarget.ABOUT);
    }

    private void navigateTo(NavigationTarget target) {
        updateNavigationSelection(target);
        if (target == NavigationTarget.DASHBOARD) {
            showDashboardView();
            return;
        }
        if (target == NavigationTarget.STATIONS) {
            authorization.require(Permission.MANAGE_STATIONS);
            showStationsView();
            return;
        }
        if (target == NavigationTarget.PRODUCTS) {
            authorization.require(Permission.MANAGE_PRODUCTS);
            showProductsView();
            return;
        }
        if (target == NavigationTarget.USERS) {
            authorization.require(Permission.MANAGE_USERS);
            showUsersView();
            return;
        }
        if (target == NavigationTarget.REPORTS) {
            authorization.require(Permission.VIEW_REPORTS);
            showReportsView();
            return;
        }
        if (target == NavigationTarget.SETTINGS) {
            authorization.require(Permission.MANAGE_SETTINGS);
            showSettingsView();
            return;
        }
        if (target == NavigationTarget.ACTIVE_SESSIONS) {
            showActiveSessionsView();
            return;
        }

        hideFeatureViews();
        informationPage.setVisible(true);
        informationPage.setManaged(true);
        pageTitle.setText(target.getTitle());
        pageDescription.setText(target.getDescription());
    }

    private void updateNavigationSelection(NavigationTarget target) {
        List<Button> navigationButtons = List.of(
                dashboardNavigationButton,
                welcomeNavigationButton,
                stationsNavigationButton,
                productsNavigationButton,
                usersNavigationButton,
                reportsNavigationButton,
                settingsNavigationButton,
                activeSessionsNavigationButton,
                aboutNavigationButton);
        navigationButtons.forEach(button ->
                button.getStyleClass().remove("navigation-button-active"));
        Button selected = navigationButtonFor(target);
        if (selected != null) {
            selected.getStyleClass().add("navigation-button-active");
        }
    }

    private Button navigationButtonFor(NavigationTarget target) {
        switch (target) {
            case DASHBOARD:
                return dashboardNavigationButton;
            case WELCOME:
                return welcomeNavigationButton;
            case STATIONS:
                return stationsNavigationButton;
            case PRODUCTS:
                return productsNavigationButton;
            case USERS:
                return usersNavigationButton;
            case REPORTS:
                return reportsNavigationButton;
            case SETTINGS:
                return settingsNavigationButton;
            case ACTIVE_SESSIONS:
                return activeSessionsNavigationButton;
            case ABOUT:
                return aboutNavigationButton;
            default:
                return null;
        }
    }

    private void showStationsView() {
        if (stationsView == null) {
            stationsView = loadStationsView();
            contentArea.getChildren().add(stationsView);
        } else {
            stationsController.refresh();
        }
        hideFeatureViews();
        informationPage.setVisible(false);
        informationPage.setManaged(false);
        stationsView.setManaged(true);
        stationsView.setVisible(true);
    }

    private Parent loadStationsView() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(STATIONS_VIEW));
        stationsController = new StationsController(
                createStationUseCase,
                updateStationUseCase,
                getStationsUseCase,
                setStationEnabledUseCase,
                getActiveSessionsUseCase,
                displayService,
                errorHandler);
        loader.setController(stationsController);
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Stations view", exception);
        }
    }

    private void showProductsView() {
        if (productsView == null) {
            productsView = loadProductsView();
            contentArea.getChildren().add(productsView);
        } else {
            productsController.refresh();
        }
        hideFeatureViews();
        informationPage.setVisible(false);
        informationPage.setManaged(false);
        productsView.setManaged(true);
        productsView.setVisible(true);
    }

    private Parent loadProductsView() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(PRODUCTS_VIEW));
        productsController = new ProductsController(
                createProductUseCase,
                updateProductUseCase,
                getProductsUseCase,
                setProductEnabledUseCase,
                updateProductStockUseCase,
                displayService,
                errorHandler);
        loader.setController(productsController);
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Products view", exception);
        }
    }

    private void showUsersView() {
        if (usersView == null) {
            usersView = loadUsersView();
            contentArea.getChildren().add(usersView);
        } else {
            usersController.refresh();
        }
        hideFeatureViews();
        informationPage.setVisible(false);
        informationPage.setManaged(false);
        usersView.setManaged(true);
        usersView.setVisible(true);
    }

    private Parent loadUsersView() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(USERS_VIEW));
        usersController = new UsersController(
                createUserUseCase,
                getUsersUseCase,
                updateUserRoleUseCase,
                setUserEnabledUseCase,
                resetUserPasswordUseCase,
                errorHandler);
        loader.setController(usersController);
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Users view", exception);
        }
    }

    private void showReportsView() {
        if (reportsView == null) {
            reportsView = loadReportsView();
            contentArea.getChildren().add(reportsView);
        } else {
            reportsController.refresh();
        }
        hideFeatureViews();
        informationPage.setVisible(false);
        informationPage.setManaged(false);
        reportsView.setManaged(true);
        reportsView.setVisible(true);
    }

    private Parent loadReportsView() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(REPORTS_VIEW));
        reportsController = new ReportsController(
                getReportUseCase, displayService, errorHandler);
        loader.setController(reportsController);
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Reports view", exception);
        }
    }

    private void showSettingsView() {
        if (settingsView == null) {
            settingsView = loadSettingsView();
            contentArea.getChildren().add(settingsView);
        } else {
            settingsController.refresh();
        }
        hideFeatureViews();
        informationPage.setVisible(false);
        informationPage.setManaged(false);
        settingsView.setManaged(true);
        settingsView.setVisible(true);
    }

    private Parent loadSettingsView() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(SETTINGS_VIEW));
        settingsController = new SettingsController(
                getSettingsUseCase,
                updateSettingsUseCase,
                errorHandler,
                this::settingsChanged);
        loader.setController(settingsController);
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Settings view", exception);
        }
    }

    private void showActiveSessionsView() {
        if (activeSessionsView == null) {
            activeSessionsView = loadActiveSessionsView();
            contentArea.getChildren().add(activeSessionsView);
        } else {
            activeSessionsController.reloadSessions();
        }
        hideFeatureViews();
        informationPage.setVisible(false);
        informationPage.setManaged(false);
        activeSessionsView.setManaged(true);
        activeSessionsView.setVisible(true);
    }

    private Parent loadActiveSessionsView() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(ACTIVE_SESSIONS_VIEW));
        activeSessionsController = new ActiveSessionsController(
                getActiveSessionsUseCase,
                clock,
                pricingService,
                displayService,
                errorHandler);
        loader.setController(activeSessionsController);
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Active Sessions view", exception);
        }
    }

    private void hideFeatureViews() {
        setHidden(dashboardView);
        setHidden(stationsView);
        setHidden(productsView);
        setHidden(usersView);
        setHidden(reportsView);
        setHidden(settingsView);
        setHidden(activeSessionsView);
    }

    private void showDashboardView() {
        if (dashboardView == null) {
            dashboardView = loadDashboardView();
            contentArea.getChildren().add(dashboardView);
        } else {
            dashboardController.reloadDashboard();
        }
        hideFeatureViews();
        informationPage.setVisible(false);
        informationPage.setManaged(false);
        dashboardView.setManaged(true);
        dashboardView.setVisible(true);
    }

    private Parent loadDashboardView() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(DASHBOARD_VIEW));
        dashboardController = new DashboardController(
                getStationsUseCase,
                getActiveSessionsUseCase,
                startSessionUseCase,
                finishSessionUseCase,
                prepareCheckoutUseCase,
                getProductsUseCase,
                addProductToSessionUseCase,
                generateInvoiceUseCase,
                pricingService,
                displayService,
                errorHandler,
                clock);
        loader.setController(dashboardController);
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Dashboard view", exception);
        }
    }

    private void settingsChanged() {
        refreshBranding();
        onSettingsChanged.run();
        if (dashboardController != null) {
            dashboardController.reloadDashboard();
        }
        if (activeSessionsController != null) {
            activeSessionsController.reloadSessions();
        }
        if (stationsController != null) {
            stationsController.refresh();
        }
        if (productsController != null) {
            productsController.refresh();
        }
        if (reportsController != null) {
            reportsController.refresh();
        }
    }

    private void refreshBranding() {
        cafeNameLabel.setText(
                displayService.getCafeName().toUpperCase(java.util.Locale.getDefault()));
    }

    private void setHidden(Parent view) {
        if (view != null) {
            view.setVisible(false);
            view.setManaged(false);
        }
    }

    public void dispose() {
        if (dashboardController != null) {
            dashboardController.dispose();
        }
        if (activeSessionsController != null) {
            activeSessionsController.dispose();
        }
    }
}
