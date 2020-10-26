package com.superzanti.serversync.GUIJavaFX;


import com.superzanti.serversync.RefStrings;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Gui_JavaFX extends Application {

    public static StackMainMenu root = new StackMainMenu();

    @Override
    public void start(Stage primaryStage) throws Exception{
        Scene scene = new Scene(root, 400,400);
        primaryStage.setScene(scene);
        primaryStage.setTitle(RefStrings.NAME+ " - " + RefStrings.VERSION);
        primaryStage.show();
    }
    public static StackMainMenu getStackLoginPane() {
        if(root==null) {
            root=new StackMainMenu();
        }
        return root;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
