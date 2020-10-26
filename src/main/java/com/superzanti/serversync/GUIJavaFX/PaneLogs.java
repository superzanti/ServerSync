package com.superzanti.serversync.GUIJavaFX;


import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

public class PaneLogs extends BorderPane {

    private TextArea txtArea = new TextArea();
    private String txt ="";
    public PaneLogs(){
        txtArea.setEditable(false);
        this.setMargin(txtArea, new Insets(10, 10, 10, 10));
        this.setCenter(txtArea);
    }

    public void updateTextArea(String text) {
        txt += text +"\n";
        txtArea.setText(txt);
    }
}
