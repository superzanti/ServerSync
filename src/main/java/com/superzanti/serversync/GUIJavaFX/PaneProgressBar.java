package com.superzanti.serversync.GUIJavaFX;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

// GUI with progress bar and texts about updated files
public class PaneProgressBar extends VBox {

    private final Label label;
    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final Label pathLabel;
    private final HBox hBoxBar;

    public PaneProgressBar() {
        this.setSpacing(5);
        this.setAlignment(Pos.CENTER);
        label = new Label("");
        
        progressBar = new ProgressBar(0);
        progressBar.setProgress(0);
        progressBar.prefWidthProperty().bind(this.widthProperty().subtract(250));

        hBoxBar = new HBox();
        hBoxBar.getChildren().addAll(label, progressBar);
        hBoxBar.setAlignment(Pos.CENTER);

        pathLabel = new Label("");

        statusLabel = new Label("");
        statusLabel.setTextFill(Color.BLUE);

        this.getChildren().addAll(hBoxBar, statusLabel, pathLabel);

    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public Label getStatusLabel() {
        return this.statusLabel;
    }

    public void setStatusText(String value) {
        this.statusLabel.textProperty().set(value);
    }

    public Label getPathLabel() {
        return this.pathLabel;
    }

    public void setPathText(String value) {
        this.pathLabel.textProperty().set(value);
    }
}
