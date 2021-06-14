package com.superzanti.serversync.GUIJavaFX;

import com.superzanti.serversync.util.Logger;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

// GUI for showing the logs
public class PaneLogs extends BorderPane {

    private final TextArea textArea = new TextArea();

    public PaneLogs() {
        textArea.setEditable(false);
        setMargin(textArea, new Insets(10, 10, 10, 10));
        this.setCenter(textArea);
        Logger.attachOutputToLogsPane(this);
    }

    public TextArea getText() {
        return textArea;
    }

    public void updateLogsArea(String text) {
        textArea.appendText(text);
    }
}
