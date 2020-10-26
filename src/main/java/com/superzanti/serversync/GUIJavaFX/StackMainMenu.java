package com.superzanti.serversync.GUIJavaFX;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;

public class StackMainMenu extends BorderPane {

    private PaneMainMenu menu = new PaneMainMenu();
    private PaneLogs logs = new PaneLogs();
    private PaneOptions options = new PaneOptions();

    private StackPane stack = new StackPane();
    private ArrayList<Button> buttons= new ArrayList<Button>();
    private String defaultStyle = new Button().getStyle();

    public StackMainMenu() {
        TabPane tabPane = new TabPane();
        Tab tabSync = new Tab("Sync");
        tabSync.setOnSelectionChanged(event -> {
            if (tabSync.isSelected()) {
                displayPanel(0);
            }
        });
        Tab tabLogs = new Tab("Logs");
        tabLogs.setOnSelectionChanged(event -> {
            if (tabLogs.isSelected()) {
                displayPanel(1);
            }
        });
        Tab tabOptions = new Tab("Options");
        tabOptions.setOnSelectionChanged(event -> {
            if (tabOptions.isSelected()) {
                displayPanel(2);
            }
        });

        tabPane.getTabs().add(tabSync);
        tabPane.getTabs().add(tabLogs);
        tabPane.getTabs().add(tabOptions);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        this.setTop(tabPane);

        options.setVisible(false);
        stack.getChildren().addAll(menu, logs, options);

        this.setCenter(stack);

        displayPanel(0);
    }
    public PaneLogs getPaneLogs() {
        if(logs==null) {
            logs = new PaneLogs();
        }
        return logs;
    }
    private void displayPanel(int n) {
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