package com.gamecafe.gamecafemanager.presentation;

import com.gamecafe.gamecafemanager.core.database.Database;
import com.gamecafe.gamecafemanager.data.repository.SQLiteProductRepository;
import com.gamecafe.gamecafemanager.data.repository.SQLiteReportRepository;
import com.gamecafe.gamecafemanager.data.repository.SQLiteSessionProductRepository;
import com.gamecafe.gamecafemanager.data.repository.SQLiteSessionRepository;
import com.gamecafe.gamecafemanager.data.repository.SQLiteSettingsRepository;
import com.gamecafe.gamecafemanager.data.repository.SQLiteStationRepository;
import com.gamecafe.gamecafemanager.data.repository.SQLiteUserRepository;
import com.gamecafe.gamecafemanager.data.security.Pbkdf2PasswordHasher;
import com.gamecafe.gamecafemanager.data.sqlite.SQLiteDatabase;
import com.gamecafe.gamecafemanager.domain.model.ApplicationSettings;
import com.gamecafe.gamecafemanager.domain.model.User;
import com.gamecafe.gamecafemanager.domain.repository.ProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.ReportRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.repository.SettingsRepository;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import com.gamecafe.gamecafemanager.domain.repository.UserRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthenticationService;
import com.gamecafe.gamecafemanager.domain.service.ApplicationSettingsService;
import com.gamecafe.gamecafemanager.domain.service.ApplicationSettingsValidator;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import com.gamecafe.gamecafemanager.domain.service.CheckoutService;
import com.gamecafe.gamecafemanager.domain.service.InvoiceService;
import com.gamecafe.gamecafemanager.domain.service.PasswordHasher;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.domain.service.ProductValidator;
import com.gamecafe.gamecafemanager.domain.service.StationValidator;
import com.gamecafe.gamecafemanager.domain.service.UserValidator;
import com.gamecafe.gamecafemanager.domain.service.pricing.GamingPricePolicy;
import com.gamecafe.gamecafemanager.domain.service.pricing.MonetaryRoundingPolicy;
import com.gamecafe.gamecafemanager.domain.service.pricing.SettingsBillableDurationPolicy;
import com.gamecafe.gamecafemanager.domain.usecase.auth.HasUsersUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.auth.InitializeAdminUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.auth.LoginUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.auth.LogoutUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.auth.RestoreAuthenticationUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.invoice.GenerateInvoiceUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.CreateProductUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.GetProductsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.SetProductEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.UpdateProductStockUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.UpdateProductUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.report.GetReportUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.settings.GetSettingsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.settings.UpdateSettingsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.FinishSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.GetActiveSessionsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.PrepareCheckoutUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.StartSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.sessionproduct.AddProductToSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.CreateStationUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.GetStationsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.SetStationEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.UpdateStationUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.CreateUserUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.GetUsersUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.ResetUserPasswordUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.SetUserEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.UpdateUserRoleUseCase;
import com.gamecafe.gamecafemanager.presentation.controller.LoginController;
import com.gamecafe.gamecafemanager.presentation.controller.MainController;
import com.gamecafe.gamecafemanager.presentation.error.ApplicationErrorHandler;
import com.gamecafe.gamecafemanager.presentation.error.ApplicationErrorMapper;
import com.gamecafe.gamecafemanager.presentation.format.ApplicationDisplayService;
import com.gamecafe.gamecafemanager.presentation.style.UiStyles;
import java.io.IOException;
import java.time.Clock;
import java.time.ZoneId;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application entry point and dependency composition root.
 */
public class GameCafeApplication extends Application {

    private static final String LOGIN_VIEW =
            "/com/gamecafe/gamecafemanager/presentation/view/login-view.fxml";
    private static final String MAIN_VIEW =
            "/com/gamecafe/gamecafemanager/presentation/view/main-view.fxml";

    private Database database;
    private Stage primaryStage;
    private StationRepository stationRepository;
    private SessionRepository sessionRepository;
    private SessionProductRepository sessionProductRepository;
    private ProductRepository productRepository;
    private ReportRepository reportRepository;
    private SettingsRepository settingsRepository;
    private UserRepository userRepository;
    private PasswordHasher passwordHasher;
    private AuthenticationService authenticationService;
    private AuthorizationService authorizationService;
    private StationValidator stationValidator;
    private ProductValidator productValidator;
    private UserValidator userValidator;
    private ApplicationSettingsValidator settingsValidator;
    private ApplicationSettingsService settingsService;
    private ApplicationDisplayService displayService;
    private ApplicationErrorHandler errorHandler;
    private PricingService pricingService;
    private CheckoutService checkoutService;
    private InvoiceService invoiceService;
    private Clock clock;

    @Override
    public void init() {
        database = SQLiteDatabase.createDefault();
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        errorHandler = new ApplicationErrorHandler(
                new ApplicationErrorMapper(), this::applicationName);
        Thread.currentThread().setUncaughtExceptionHandler(
                (thread, failure) -> errorHandler.show(
                        primaryStage, "Unexpected application error", failure));
        try {
            database.initialize();
            configureDependencies();
            applySettings();
            new RestoreAuthenticationUseCase(authenticationService)
                    .execute()
                    .ifPresentOrElse(this::showMain, this::showLogin);
            primaryStage.show();
        } catch (RuntimeException exception) {
            errorHandler.show(null, "Application could not start", exception);
            Platform.exit();
        }
    }

    private void configureDependencies() {
        stationRepository = new SQLiteStationRepository(database);
        sessionRepository = new SQLiteSessionRepository(database);
        sessionProductRepository = new SQLiteSessionProductRepository(database);
        productRepository = new SQLiteProductRepository(database);
        reportRepository = new SQLiteReportRepository(database);
        settingsRepository = new SQLiteSettingsRepository(database);
        userRepository = new SQLiteUserRepository(database);
        passwordHasher = new Pbkdf2PasswordHasher();
        authenticationService = new AuthenticationService(userRepository, passwordHasher);
        authorizationService = new AuthorizationService(authenticationService);
        stationValidator = new StationValidator();
        productValidator = new ProductValidator();
        userValidator = new UserValidator();
        settingsValidator = new ApplicationSettingsValidator();
        settingsService = new ApplicationSettingsService(settingsRepository);
        displayService = new ApplicationDisplayService(settingsService);
        pricingService = new PricingService(
                new SettingsBillableDurationPolicy(settingsService),
                GamingPricePolicy.proratedHourlyRate(),
                MonetaryRoundingPolicy.standardCurrency());
        checkoutService = new CheckoutService(pricingService);
        invoiceService = new InvoiceService(settingsService);
        clock = Clock.systemUTC();
    }

    private void showLogin() {
        LoginController controller = new LoginController(
                new HasUsersUseCase(userRepository),
                new InitializeAdminUseCase(userRepository, passwordHasher, userValidator),
                new LoginUseCase(authenticationService),
                displayService,
                errorHandler,
                this::showMain);
        FXMLLoader loader = new FXMLLoader(GameCafeApplication.class.getResource(LOGIN_VIEW));
        loader.setController(controller);
        primaryStage.setMinWidth(620.0);
        primaryStage.setMinHeight(480.0);
        primaryStage.setScene(createScene(load(loader, "Login"), 720.0, 540.0));
    }

    private void showMain(User user) {
        GetStationsUseCase getStations = new GetStationsUseCase(stationRepository);
        GetProductsUseCase getProducts = new GetProductsUseCase(productRepository);
        GetActiveSessionsUseCase getActiveSessions =
                new GetActiveSessionsUseCase(sessionRepository, authorizationService);
        MainController mainController = new MainController(
                new CreateStationUseCase(
                        stationRepository, stationValidator, authorizationService),
                new UpdateStationUseCase(
                        stationRepository, stationValidator, authorizationService),
                getStations,
                new SetStationEnabledUseCase(stationRepository, authorizationService),
                new CreateProductUseCase(
                        productRepository, productValidator, authorizationService),
                new UpdateProductUseCase(
                        productRepository, productValidator, authorizationService),
                getProducts,
                new SetProductEnabledUseCase(productRepository, authorizationService),
                new UpdateProductStockUseCase(
                        productRepository, productValidator, authorizationService),
                getActiveSessions,
                new StartSessionUseCase(
                        stationRepository, sessionRepository, clock, authorizationService),
                new FinishSessionUseCase(
                        sessionRepository,
                        sessionProductRepository,
                        checkoutService,
                        clock,
                        authorizationService),
                new PrepareCheckoutUseCase(
                        sessionRepository,
                        sessionProductRepository,
                        checkoutService,
                        clock,
                        authorizationService),
                new AddProductToSessionUseCase(
                        sessionRepository, sessionProductRepository, authorizationService),
                new GenerateInvoiceUseCase(
                        sessionRepository,
                        sessionProductRepository,
                        invoiceService,
                        authorizationService),
                new GetReportUseCase(
                        reportRepository,
                        authorizationService,
                        clock,
                        ZoneId.systemDefault()),
                new GetSettingsUseCase(settingsService, authorizationService),
                new UpdateSettingsUseCase(
                        settingsService, settingsValidator, authorizationService),
                new CreateUserUseCase(
                        userRepository, passwordHasher, userValidator, authorizationService),
                new GetUsersUseCase(userRepository, authorizationService),
                new UpdateUserRoleUseCase(userRepository, authorizationService),
                new SetUserEnabledUseCase(userRepository, authorizationService),
                new ResetUserPasswordUseCase(
                        userRepository, passwordHasher, userValidator, authorizationService),
                pricingService,
                displayService,
                errorHandler,
                clock,
                authorizationService,
                new LogoutUseCase(authenticationService),
                user,
                this::applySettings,
                this::showLogin);

        FXMLLoader loader = new FXMLLoader(GameCafeApplication.class.getResource(MAIN_VIEW));
        loader.setControllerFactory(controllerType -> {
            if (controllerType == MainController.class) {
                return mainController;
            }
            throw new IllegalArgumentException("Unsupported controller " + controllerType.getName());
        });
        primaryStage.setMinWidth(800.0);
        primaryStage.setMinHeight(500.0);
        primaryStage.setScene(createScene(load(loader, "Main"), 1060.0, 680.0));
    }

    private void applySettings() {
        primaryStage.setTitle(displayService.getCafeName());
    }

    private String applicationName() {
        return displayService == null
                ? ApplicationSettings.defaults().getCafeName()
                : displayService.getCafeName();
    }

    private Parent load(FXMLLoader loader, String viewName) {
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load " + viewName + " view", exception);
        }
    }

    private Scene createScene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        UiStyles.apply(scene);
        return scene;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
