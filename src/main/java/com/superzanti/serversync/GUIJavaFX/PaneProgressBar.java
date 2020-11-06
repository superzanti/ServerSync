package com.superzanti.serversync.GUIJavaFX;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

public class PaneProgressBar extends HBox {

    private Label label;
    private ProgressBar progressBar;
    private Label statusLabel;
    private String text;

    public PaneProgressBar(){
        this.setSpacing(5);
        this.setAlignment(Pos.CENTER);
        label =  new Label("Copy files:");
        progressBar = new ProgressBar(0);
        statusLabel = new Label("");

        //statusLabel.setMaxWidth(250);
        statusLabel.setTextFill(Color.BLUE);
        progressBar.setProgress(0);
        progressBar.prefWidthProperty().bind(this.widthProperty().subtract(250));
        this.getChildren().addAll(label,progressBar);
    }


    public Label getLabel() {
        return label;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public void setText(String string) {
        this.text = string;
    }
    public void update() {
        this.statusLabel.setText(this.text);
    }
}
