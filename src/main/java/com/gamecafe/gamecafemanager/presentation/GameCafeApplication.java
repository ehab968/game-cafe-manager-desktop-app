package com.gamecafe.gamecafemanager.presentation;

import com.gamecafe.gamecafemanager.core.database.Database;
import com.gamecafe.gamecafemanager.data.repository.SQLiteSessionRepository;
import com.gamecafe.gamecafemanager.data.repository.SQLiteStationRepository;
import com.gamecafe.gamecafemanager.data.sqlite.SQLiteDatabase;
import com.gamecafe.gamecafemanager.domain.repository.SessionRepository;
import com.gamecafe.gamecafemanager.domain.repository.StationRepository;
import com.gamecafe.gamecafemanager.domain.service.StationValidator;
import com.gamecafe.gamecafemanager.domain.usecase.session.GetActiveSessionsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.CreateStationUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.GetStationsUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.SetStationEnabledUseCase;
import com.gamecafe.gamecafemanager.domain.usecase.station.UpdateStationUseCase;
import com.gamecafe.gamecafemanager.presentation.controller.MainController;
import java.io.IOException;
import java.time.Clock;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application entry point.
 */
public class GameCafeApplication extends Application {

    private static final String MAIN_VIEW =
            "/com/gamecafe/gamecafemanager/presentation/view/main-view.fxml";

    private Database database;

    @Override
    public void init() {
        database = SQLiteDatabase.createDefault();
        database.initialize();
    }

    @Override
    public void start(Stage stage) throws IOException {
        StationRepository stationRepository = new SQLiteStationRepository(database);
        SessionRepository sessionRepository = new SQLiteSessionRepository(database);
        StationValidator stationValidator = new StationValidator();
        GetActiveSessionsUseCase getActiveSessionsUseCase =
                new GetActiveSessionsUseCase(sessionRepository);
        MainController mainController = new MainController(
                new CreateStationUseCase(stationRepository, stationValidator),
                new UpdateStationUseCase(stationRepository, stationValidator),
                new GetStationsUseCase(stationRepository),
                new SetStationEnabledUseCase(stationRepository),
                getActiveSessionsUseCase,
                Clock.systemUTC());

        FXMLLoader loader = new FXMLLoader(GameCafeApplication.class.getResource(MAIN_VIEW));
        loader.setControllerFactory(controllerType -> {
            if (controllerType == MainController.class) {
                return mainController;
            }
            throw new IllegalArgumentException("Unsupported controller " + controllerType.getName());
        });
        Parent root = loader.load();
        Scene scene = new Scene(root, 960, 600);

        stage.setTitle("Game Cafe Manager");
        stage.setMinWidth(800);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
