package com.superzanti.serversync.GUIJavaFX;

import com.superzanti.serversync.util.ServerSyncLogger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

// GUI for showing the logs
public class PaneLogs extends BorderPane {

    private final TextArea txtArea = new TextArea();

    public PaneLogs() {
        Logger log = ServerSyncLogger.getLog();
        log.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                Platform.runLater(() -> updateLogsArea(ServerSyncLogger.formatter.format(record)));
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        });

        txtArea.setEditable(false);
        setMargin(txtArea, new Insets(10, 10, 10, 10));
        this.setCenter(txtArea);
    }

    public void updateLogsArea(String text) {
        txtArea.appendText(text);
    }
}
