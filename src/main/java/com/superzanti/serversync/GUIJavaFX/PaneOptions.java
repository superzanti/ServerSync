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

public class PaneOptions extends GridPane {

    public PaneOptions(){
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(10,10,10,10));
        /** CLIENT */
        Label labelClient = new Label("Client (Options by default):");
        labelClient.setFont(new Font("Arial", 16));
        this.setRowIndex(labelClient, 0);
        this.setColumnIndex(labelClient, 0);

        Label labelTheme = new Label("Theme: ");
        this.setRowIndex(labelTheme, 1);
        this.setColumnIndex(labelTheme, 0);

        ObservableList<? extends Serializable> themes =
                FXCollections.observableArrayList(
                        EThemes.DARK_CYAN.name(),
                        EThemes.DARK_YELLOW.name()
                );
        ComboBox comboBox = new ComboBox(themes);
        comboBox.getSelectionModel().select(0);
        comboBox.valueProperty().addListener((obs, oldItem, newItem) -> {
            for (EThemes theme : EThemes.values()) {
                if(newItem == theme.name()){
                    Gui_JavaFX.root.setStyle(theme.toString());
                    break;
                }
            }
        });
        this.setRowIndex(comboBox, 1);
        this.setColumnIndex(comboBox, 1);

        this.getChildren().addAll(labelTheme,comboBox);

        Label labelIp = new Label("IP: ");
        this.setRowIndex(labelIp, 2);
        this.setColumnIndex(labelIp, 0);

        TextField fieldIp = new TextField();
        fieldIp.setText(SyncConfig.getConfig().SERVER_IP);
        this.setRowIndex(fieldIp, 2);
        this.setColumnIndex(fieldIp, 1);

        Label labelPort = new Label("Port: ");
        this.setRowIndex(labelPort, 3);
        this.setColumnIndex(labelPort, 0);

        TextField fieldPort = new TextField();
        fieldPort.setText(String.valueOf(SyncConfig.getConfig().SERVER_PORT));
        this.setRowIndex(fieldPort, 3);
        this.setColumnIndex(fieldPort, 1);

        Label labelRefuse = new Label("Refuse client mods: ");
        this.setRowIndex(labelRefuse, 4);
        this.setColumnIndex(labelRefuse, 0);

        CheckBox cbxRefuse = new CheckBox();
        if(SyncConfig.getConfig().REFUSE_CLIENT_MODS){
            cbxRefuse.setSelected(true);
        }else{
            cbxRefuse.setSelected(false);
        }
        this.setRowIndex(cbxRefuse, 4);
        this.setColumnIndex(cbxRefuse, 1);

        Label labelIgnore = new Label("Ignore list: ");
        this.setRowIndex(labelIgnore, 5);
        this.setColumnIndex(labelIgnore, 0);

        ListView<String> ignoreList = new ListView<String>();
        ObservableList<String> items = FXCollections.observableArrayList (SyncConfig.getConfig().FILE_IGNORE_LIST);
        ignoreList.setItems(items);
        this.setRowIndex(ignoreList, 6);
        this.setColumnIndex(ignoreList, 0);

        this.getChildren().addAll(labelClient, labelIp, labelPort, labelRefuse, cbxRefuse, fieldIp, fieldPort);
        this.getChildren().addAll(labelIgnore, ignoreList);

        /** CANCEL BUTTON */
        Button btnCancel = new Button("Cancel");
        btnCancel.getStyleClass().add("btn");
        btnCancel.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                Gui_JavaFX.root.setStyle(EThemes.DARK_YELLOW.toString());
            }
        });
        this.setRowIndex(btnCancel, 7);
        this.setColumnIndex(btnCancel, 0);
        this.setHalignment(btnCancel, HPos.RIGHT);

        /** SAVE BUTTON */
        Button btnSave = new Button("Save");
        btnSave.getStyleClass().add("btn");
        btnSave.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                int port = Integer.parseInt(fieldPort.getText());
                String ip = fieldIp.getText();
                if(Gui_JavaFX.getStackMainPane().getPaneSync().setPort(port)){
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

        this.setRowIndex(btnSave, 7);
        this.setColumnIndex(btnSave, 1);

        this.getChildren().addAll(btnCancel, btnSave);
    }
    public void updateLogsArea(String text) {
        Gui_JavaFX.getStackMainPane().getPaneLogs().updateLogsArea(text);
    }
}
