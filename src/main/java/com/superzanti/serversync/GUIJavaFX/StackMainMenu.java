package com.superzanti.serversync.GUIJavaFX;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class StackMainMenu extends StackPane{

    public StackMainMenu() {
        PaneMainMenu menu = new PaneMainMenu();
        this.getChildren().addAll(menu);
    }

}