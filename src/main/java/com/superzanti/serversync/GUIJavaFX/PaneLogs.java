package com.superzanti.serversync.GUIJavaFX;

import com.superzanti.serversync.util.Log;
import com.superzanti.serversync.util.Logger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

// GUI of logs
public class PaneLogs extends BorderPane {

    private final TextArea txtArea = new TextArea();

    public PaneLogs() {
        Logger.getLog().addObserver((o, arg) -> {
            if (o instanceof Log) {
                Platform.runLater(() -> updateLogsArea(((Log) o).userFacingLog.toString()));
            }
        });
        txtArea.setEditable(false);
        setMargin(txtArea, new Insets(10, 10, 10, 10));
        this.setCenter(txtArea);
    }

    public void updateLogsArea(String text) {
        txtArea.setText(text);
        txtArea.setScrollTop(Double.MAX_VALUE);
        txtArea.selectPositionCaret(txtArea.getLength());
        txtArea.deselect(); //removes the highlighting
    }

}
