package com.superzanti.serversync.GUIJavaFX;

import com.superzanti.serversync.RefStrings;
import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.client.ClientWorker;
import com.superzanti.serversync.client.EActionType;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.Then;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.concurrent.Callable;

// Main class of the GUI, launch the window
public class ProgressModeGUI extends Application {

    private static StackMainMenu root = new StackMainMenu();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        root.setStyle(SyncConfig.getConfig().THEME.toString());

        Scene scene = new Scene(root, 1024, 576);
        scene.getStylesheets().add(this.getClass().getResource("/css/application.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle(RefStrings.NAME + " - " + RefStrings.VERSION);
        primaryStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
        root.getPaneSync().setCloseOnSuccessfulSync(true);
        root.getPaneSync().getBtnSync().fire();
    }
}
