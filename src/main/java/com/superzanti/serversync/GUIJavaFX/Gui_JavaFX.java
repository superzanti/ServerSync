package com.superzanti.serversync.GUIJavaFX;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Gui_JavaFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

        StackMainMenu root = new StackMainMenu();
        Scene scene = new Scene(root, 400,400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("ServerSync");
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
