package com.superzanti.serversync.GUIJavaFX;

import com.superzanti.serversync.config.ConfigLoader;
import com.superzanti.serversync.config.JsonConfig;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.util.enums.EThemes;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;

import java.io.IOException;
import java.io.Serializable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// GUI of options
public class PaneOptions extends GridPane {

    public PaneOptions() {
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(10, 10, 10, 10));

        /* CLIENT */
        /* CLIENT -> Themes*/
        Label labelClient = new Label("Client (Options by default):");
        labelClient.setFont(new Font("Arial", 16));
        setRowIndex(labelClient, 0);
        setColumnIndex(labelClient, 0);

        Label labelTheme = new Label("Theme: ");
        setRowIndex(labelTheme, 1);
        setColumnIndex(labelTheme, 0);

        ObservableList<? extends Serializable> themes =
                FXCollections.observableArrayList(
                        Stream.of(EThemes.values())
                                .map(Enum::name)
                                .collect(Collectors.toList())
                );
        ComboBox comboBox = new ComboBox(themes);
        comboBox.getSelectionModel().select(0);
        comboBox.valueProperty().addListener((obs, oldItem, newItem) -> {
            for (EThemes theme : EThemes.values()) {
                if (newItem == theme.name()) {
                    Gui_JavaFX.getStackMainPane().setStyle(theme.toString());
                    break;
                }
            }
        });
        setRowIndex(comboBox, 1);
        setColumnIndex(comboBox, 1);

        this.getChildren().addAll(labelTheme, comboBox);

        /* CLIENT -> IP*/
        Label labelIp = new Label("IP: ");
        setRowIndex(labelIp, 2);
        setColumnIndex(labelIp, 0);

        TextField fieldIp = new TextField();
        fieldIp.setText(SyncConfig.getConfig().SERVER_IP);
        setRowIndex(fieldIp, 2);
        setColumnIndex(fieldIp, 1);

        /* CLIENT -> PORT*/
        Label labelPort = new Label("Port: ");
        setRowIndex(labelPort, 3);
        setColumnIndex(labelPort, 0);

        TextField fieldPort = new TextField();
        fieldPort.setText(String.valueOf(SyncConfig.getConfig().SERVER_PORT));
        setRowIndex(fieldPort, 3);
        setColumnIndex(fieldPort, 1);

        /* CLIENT -> Refuse client mods*/
        Label labelRefuse = new Label("Refuse client mods: ");
        setRowIndex(labelRefuse, 4);
        setColumnIndex(labelRefuse, 0);

        CheckBox cbxRefuse = new CheckBox();
        cbxRefuse.setSelected(SyncConfig.getConfig().REFUSE_CLIENT_MODS);
        setRowIndex(cbxRefuse, 4);
        setColumnIndex(cbxRefuse, 1);

        /* CLIENT -> Ignore list*/
        Label labelIgnore = new Label("Ignore list: ");
        setRowIndex(labelIgnore, 5);
        setColumnIndex(labelIgnore, 0);

        ListView<String> ignoreList = new ListView<String>();
        ObservableList<String> items = FXCollections.observableArrayList(SyncConfig.getConfig().FILE_IGNORE_LIST);
        ignoreList.setItems(items);
        setRowIndex(ignoreList, 6);
        setColumnIndex(ignoreList, 0);

        this.getChildren().addAll(labelClient, labelIp, labelPort, labelRefuse, cbxRefuse, fieldIp, fieldPort);
        this.getChildren().addAll(labelIgnore, ignoreList);

        /* CANCEL BUTTON */
        Button btnCancel = new Button("Cancel");
        btnCancel.getStyleClass().add("btn");
        btnCancel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                fieldIp.setText(SyncConfig.getConfig().SERVER_IP);
                fieldPort.setText(String.valueOf(SyncConfig.getConfig().SERVER_PORT));
                cbxRefuse.setSelected(SyncConfig.getConfig().REFUSE_CLIENT_MODS);
            }
        });
        setRowIndex(btnCancel, 7);
        setColumnIndex(btnCancel, 0);
        setHalignment(btnCancel, HPos.RIGHT);

        /* SAVE BUTTON */
        Button btnSave = new Button("Save");
        btnSave.getStyleClass().add("btn");
        btnSave.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                int port = Integer.parseInt(fieldPort.getText());
                String ip = fieldIp.getText();
                if (Gui_JavaFX.getStackMainPane().getPaneSync().setPort(port)) {
                    Gui_JavaFX.getStackMainPane().getPaneSync().setIPAddress(ip);

                    SyncConfig.getConfig().SERVER_IP = ip;
                    SyncConfig.getConfig().SERVER_PORT = port;
                    SyncConfig.getConfig().REFUSE_CLIENT_MODS = cbxRefuse.isSelected();

                    try {
                        JsonConfig.saveClient(ConfigLoader.v2ClientConfig);
                        updateLogsArea("Options saved");
                        //btnSave.setDisable(true);
                    } catch (IOException ioException) {
                        updateLogsArea(ioException.toString());
                    }
                }
            }
        });

        setRowIndex(btnSave, 7);
        setColumnIndex(btnSave, 1);

        this.getChildren().addAll(btnCancel, btnSave);
    }

    public void updateLogsArea(String text) {
        Gui_JavaFX.getStackMainPane().getPaneLogs().updateLogsArea(text);
    }
}
