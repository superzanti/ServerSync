package com.superzanti.serversync.GUIJavaFX;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

// Store all panels
public class StackMainMenu extends BorderPane {

    private PaneSync sync = getPaneSync();
    private PaneLogs logs = getPaneLogs();
    private PaneOptions options = getPaneOptions();
    private PaneUpdate update = getPaneUpdate();
    private PaneSideBar sideBar = new PaneSideBar();

    private final StackPane stack = new StackPane();

    public StackMainMenu() {

        this.setLeft(sideBar);

        options.setVisible(false);
        stack.getChildren().addAll(sync, logs, options, update);

        this.setCenter(stack);

        displayPanel(0);
    }
    public PaneSideBar getPaneSideBar() {
        if (sideBar == null) {
            sideBar = new PaneSideBar();
        }
        return sideBar;
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
    public PaneUpdate getPaneUpdate() {
        if (update == null) {
            update = new PaneUpdate();
        }
        return update;
    }

    public void displayPanel(int n) {
        if (stack.getChildren().size() > 0) {
            for (Node node : stack.getChildren()) {
                node.setVisible(false);
            }
        stack.getChildren().get(n).setVisible(true);
        }

    }
}