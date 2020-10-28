package com.superzanti.serversync.GUIJavaFX;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;


import java.util.ArrayList;

public class StackMainMenu extends BorderPane {

    private PaneSync sync = new PaneSync();
    private PaneLogs logs = new PaneLogs();
    private PaneOptions options = new PaneOptions();

    private StackPane stack = new StackPane();
    private ArrayList<Button> buttons= new ArrayList<Button>();
    private String defaultStyle = new Button().getStyle();

    public StackMainMenu() {
        PaneSideBar sideBar = new PaneSideBar();
        this.setLeft(sideBar);

        options.setVisible(false);
        stack.getChildren().addAll(sync, logs, options);

        this.setCenter(stack);

        displayPanel(0);
    }
    public PaneLogs getPaneLogs() {
        if(logs==null) {
            logs = new PaneLogs();
        }
        return logs;
    }
    public PaneSync getPaneSync() {
        if(sync==null) {
            sync = new PaneSync();
        }
        return sync;
    }
    public void displayPanel(int n) {
        if(stack.getChildren().size() >0){
            for(Node node: stack.getChildren()) {
                node.setVisible(false);
            }
        /*for(Button btn: buttons) {
            btn.setStyle("-fx-background-color: #A9A9A9");;
        }
        buttons.get(n).setStyle(defaultStyle);;*/
            stack.getChildren().get(n).setVisible(true);
        }

    }
}