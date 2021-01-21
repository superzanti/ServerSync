package com.superzanti.serversync.GUIJavaFX;

import com.superzanti.serversync.RefStrings;
import com.superzanti.serversync.config.SyncConfig;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

// Main class of the GUI, launch the window
public class Gui_JavaFX extends Application {

    private static StackMainMenu root = new StackMainMenu();

    public static StackMainMenu getStackMainPane() {
        if (root == null) {
            root = new StackMainMenu();
        }
        return root;
    }

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
    }
}
