package com.gamecafe.gamecafemanager.data.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamecafe.gamecafemanager.data.security.Pbkdf2PasswordHasher;
import com.gamecafe.gamecafemanager.data.sqlite.SQLiteDatabase;
import com.gamecafe.gamecafemanager.domain.exception.AuthorizationException;
import com.gamecafe.gamecafemanager.domain.model.Permission;
import com.gamecafe.gamecafemanager.domain.model.Product;
import com.gamecafe.gamecafemanager.domain.model.Role;
import com.gamecafe.gamecafemanager.domain.model.Session;
import com.gamecafe.gamecafemanager.domain.model.SessionStatus;
import com.gamecafe.gamecafemanager.domain.model.Station;
import com.gamecafe.gamecafemanager.domain.model.StationType;
import com.gamecafe.gamecafemanager.domain.repository.ProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionProductRepository;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import com.gamecafe.gamecafemanager.domain.repository.UserRepository;
import com.gamecafe.gamecafemanager.domain.service.AuthenticationService;
import com.gamecafe.gamecafemanager.domain.service.AuthorizationService;
import com.gamecafe.gamecafemanager.domain.service.CheckoutService;
import com.gamecafe.gamecafemanager.domain.service.PasswordHasher;
import com.gamecafe.gamecafemanager.domain.service.PricingService;
import com.gamecafe.gamecafemanager.domain.service.ProductValidator;
import com.gamecafe.gamecafemanager.domain.service.StationValidator;
import com.gamecafe.gamecafemanager.domain.service.UserValidator;
import com.gamecafe.gamecafemanager.domain.usecase.auth.InitializeAdminUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.CreateProductUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.product.UpdateProductStockUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.FinishSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.PrepareCheckoutUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.session.StartSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.sessionproduct.AddProductToSessionUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.CreateStationUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.CreateUserUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.user.GetUsersUseCase;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuthorizationIntegrationTest {

    private static final String ADMIN_PASSWORD = "admin-password";
    private static final String CASHIER_PASSWORD = "cashier-password";
    private static final Instant START_TIME = Instant.parse("2026-08-22T12:00:00Z");

    @TempDir
    Path temporaryDirectory;

    private StationRepository stationRepository;
    private ProductRepository productRepository;
    private SessionRepository sessionRepository;
    private SessionProductRepository sessionProductRepository;
    private UserRepository userRepository;
    private PasswordHasher passwordHasher;
    private AuthenticationService authenticationService;
    private AuthorizationService authorization;

    @BeforeEach
    void setUp() {
        SQLiteDatabase database = new SQLiteDatabase(
                temporaryDirectory.resolve("authorization.db"));
        database.initialize();
        stationRepository = new SQLiteStationRepository(database);
        productRepository = new SQLiteProductRepository(database);
        sessionRepository = new SQLiteSessionRepository(database);
        sessionProductRepository = new SQLiteSessionProductRepository(database);
        userRepository = new SQLiteUserRepository(database);
        passwordHasher = new Pbkdf2PasswordHasher();
        authenticationService = new AuthenticationService(userRepository, passwordHasher);
        authorization = new AuthorizationService(authenticationService);
        initializeAdminAndLogin();
    }

    @Test
    void adminHasAllPermissionsAndCanManageUsers() {
        for (Permission permission : Permission.values()) {
            assertTrue(authorization.isAllowed(permission));
        }

        createCashier();

        assertEquals(2, new GetUsersUseCase(userRepository, authorization).execute().size());
    }

    @Test
    void cashierIsDeniedManagementButCanOperateAndCheckoutSession() {
        Station station = new CreateStationUseCase(
                stationRepository, new StationValidator(), authorization).execute(
                        "Room 1", StationType.PLAYSTATION, new BigDecimal("120.00"));
        Product product = new CreateProductUseCase(
                productRepository, new ProductValidator(), authorization).execute(
                        "Water", new BigDecimal("10.00"), 5);
        createCashier();
        loginAsCashier();

        assertFalse(authorization.isAllowed(Permission.MANAGE_STATIONS));
        assertFalse(authorization.isAllowed(Permission.MANAGE_PRODUCTS));
        assertFalse(authorization.isAllowed(Permission.MANAGE_USERS));
        assertFalse(authorization.isAllowed(Permission.VIEW_REPORTS));
        assertFalse(authorization.isAllowed(Permission.MANAGE_SETTINGS));
        assertThrows(AuthorizationException.class, () ->
                new CreateStationUseCase(
                        stationRepository, new StationValidator(), authorization).execute(
                                "Denied", StationType.BILLIARD, BigDecimal.TEN));
        assertThrows(AuthorizationException.class, () ->
                new UpdateProductStockUseCase(
                        productRepository, new ProductValidator(), authorization)
                        .execute(product.getId(), 99));
        assertThrows(AuthorizationException.class, () ->
                new GetUsersUseCase(userRepository, authorization).execute());

        Session active = new StartSessionUseCase(
                stationRepository,
                sessionRepository,
                fixedClock(START_TIME),
                authorization).execute(station.getId());
        new AddProductToSessionUseCase(
                sessionRepository, sessionProductRepository, authorization)
                .execute(active.getId(), product.getId(), 2);
        CheckoutService checkoutService = new CheckoutService(new PricingService());
        Instant endTime = START_TIME.plusSeconds(1_800L);
        new PrepareCheckoutUseCase(
                sessionRepository,
                sessionProductRepository,
                checkoutService,
                fixedClock(endTime),
                authorization).execute(active.getId());
        Session completed = new FinishSessionUseCase(
                sessionRepository,
                sessionProductRepository,
                checkoutService,
                fixedClock(endTime),
                authorization).execute(active.getId(), endTime);

        assertEquals(SessionStatus.COMPLETED, completed.getStatus());
        assertEquals(new BigDecimal("60.00"), completed.getPlayCost());
        assertEquals(new BigDecimal("20.00"), completed.getProductsCost());
        assertEquals(new BigDecimal("80.00"), completed.getFinalTotal());
    }

    private void initializeAdminAndLogin() {
        char[] password = ADMIN_PASSWORD.toCharArray();
        try {
            new InitializeAdminUseCase(
                    userRepository, passwordHasher, new UserValidator())
                    .execute("admin", password);
            authenticationService.authenticate("admin", password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void createCashier() {
        char[] password = CASHIER_PASSWORD.toCharArray();
        try {
            new CreateUserUseCase(
                    userRepository,
                    passwordHasher,
                    new UserValidator(),
                    authorization).execute("cashier", Role.CASHIER, password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void loginAsCashier() {
        authenticationService.logout();
        char[] password = CASHIER_PASSWORD.toCharArray();
        try {
            authenticationService.authenticate("cashier", password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
