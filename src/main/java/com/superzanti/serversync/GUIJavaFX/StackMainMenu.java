package com.superzanti.serversync.GUIJavaFX;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

// Store all panels
public class StackMainMenu extends BorderPane {

    private PaneSync sync = getPaneSync();
    private PaneLogs logs = getPaneLogs();
    private PaneOptions options = getPaneOptions();

    private final StackPane stack = new StackPane();

    public StackMainMenu() {
        PaneSideBar sideBar = new PaneSideBar();
        this.setLeft(sideBar);

        options.setVisible(false);
        stack.getChildren().addAll(sync, logs, options);

        this.setCenter(stack);

        displayPanel(0);
    }

    public PaneLogs getPaneLogs() {
        if (logs == null) {
            logs = new PaneLogs();
        }
        return logs;
    }

    public PaneSync getPaneSync() {
        if (sync == null) {
            sync = new PaneSync();
        }
        return sync;
    }

    public PaneOptions getPaneOptions() {
        if (options == null) {
            options = new PaneOptions();
        }
        return options;
    }

    public void displayPanel(int n) {
        if (stack.getChildren().size() > 0) {
            for (Node node : stack.getChildren()) {
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