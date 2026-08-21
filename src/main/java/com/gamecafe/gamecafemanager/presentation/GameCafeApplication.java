package com.gamecafe.gamecafemanager.presentation;

import com.gamecafe.gamecafemanager.core.database.Database;
import com.gamecafe.gamecafemanager.data.sqlite.SQLiteDatabase;
import java.io.IOException;
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
        Parent root = FXMLLoader.load(GameCafeApplication.class.getResource(MAIN_VIEW));
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
