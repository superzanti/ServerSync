package com.superzanti.serversync.GUI;

import com.superzanti.serversync.util.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

// GUI for showing the logs
public class PaneLogs extends BorderPane {

    private final TextArea textArea = new TextArea();

    public PaneLogs() {
        textArea.setEditable(false);
        setMargin(textArea, new Insets(10, 10, 10, 10));
        this.setCenter(textArea);
        final StringProperty records = new SimpleStringProperty();
        getText().textProperty().bind(records);

        Handler handler = new Handler() {
            final SimpleFormatter fmt = new SimpleFormatter();
            final StringBuilder r = new StringBuilder();


            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().equals(Level.INFO)) {
                    r.append(fmt.format(record));
                    Logger.flush();
                }
            }

            @Override
            public void flush() {
                records.set(r.toString());
            }

            @Override
            public void close() {
            }
        };

        Logger.attachUIHandler(handler);
    }

    public TextArea getText() {
        return textArea;
    }

    public void updateLogsArea(String text) {
        textArea.appendText(text);
    }
}
