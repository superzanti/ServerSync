package com.superzanti.serversync.GUIJavaFX;

import com.superzanti.serversync.util.Logger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

// GUI for showing the logs
public class PaneLogs extends BorderPane {

    private final TextArea txtArea = new TextArea();

    public PaneLogs() {
        txtArea.setEditable(false);
        setMargin(txtArea, new Insets(10, 10, 10, 10));
        this.setCenter(txtArea);
        Logger.attachOutputToLogsPane(this);
    }

    public void updateLogsArea(String text) {
        txtArea.appendText(text);
    }
}
