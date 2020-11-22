package com.superzanti.serversync.GUIJavaFX;

import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.EThemes;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

    private final TextField address = getAddressField();
    private final TextField port = getPortField();
    private final CheckBox refuseClientMods = getRefuseClientModsCheckbox();

    public PaneOptions() {
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(10, 10, 10, 10));

        /* CLIENT */
        /* CLIENT -> Themes*/
        Label labelClient = new Label("Client configuration:");
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

        TextField fieldAddress = getAddressField();
        setRowIndex(fieldAddress, 2);
        setColumnIndex(fieldAddress, 1);

        /* CLIENT -> PORT*/
        Label labelPort = new Label("Port: ");
        setRowIndex(labelPort, 3);
        setColumnIndex(labelPort, 0);

        TextField fieldPort = getPortField();
        setRowIndex(fieldPort, 3);
        setColumnIndex(fieldPort, 1);

        /* CLIENT -> Refuse client mods*/
        Label labelRefuse = new Label("Refuse client mods: ");
        setRowIndex(labelRefuse, 4);
        setColumnIndex(labelRefuse, 0);

        CheckBox cbxRefuse = getRefuseClientModsCheckbox();
        cbxRefuse.setSelected(SyncConfig.getConfig().REFUSE_CLIENT_MODS);
        setRowIndex(cbxRefuse, 4);
        setColumnIndex(cbxRefuse, 1);

        /* CLIENT -> Ignore list*/
        // TODO implement this ignore list
//        Label labelIgnore = new Label("Ignore list: ");
//        setRowIndex(labelIgnore, 5);
//        setColumnIndex(labelIgnore, 0);
//
//        ListView<String> ignoreList = new ListView<String>();
//        ObservableList<String> items = FXCollections.observableArrayList(SyncConfig.getConfig().FILE_IGNORE_LIST);
//        ignoreList.setItems(items);
//        setRowIndex(ignoreList, 6);
//        setColumnIndex(ignoreList, 0);

        this.getChildren().addAll(labelClient, labelIp, labelPort, labelRefuse, cbxRefuse, fieldAddress, fieldPort);
//        this.getChildren().addAll(labelIgnore, ignoreList);

        /* CANCEL BUTTON */
        Button btnCancel = new Button("Cancel");
        btnCancel.getStyleClass().add("btn");
        btnCancel.setOnAction(e -> {
            fieldAddress.setText(SyncConfig.getConfig().SERVER_IP);
            fieldPort.setText(String.valueOf(SyncConfig.getConfig().SERVER_PORT));
            cbxRefuse.setSelected(SyncConfig.getConfig().REFUSE_CLIENT_MODS);
        });
        setRowIndex(btnCancel, 7);
        setColumnIndex(btnCancel, 0);
        setHalignment(btnCancel, HPos.RIGHT);

        /* SAVE BUTTON */
        Button btnSave = new Button("Save");
        btnSave.getStyleClass().add("btn");
        btnSave.setOnAction(e -> {
            int port = Integer.parseInt(fieldPort.getText());
            String ip = fieldAddress.getText();
            if (Gui_JavaFX.getStackMainPane().getPaneSync().setPort(port)) {
                Gui_JavaFX.getStackMainPane().getPaneSync().setIPAddress(ip);

                SyncConfig.getConfig().SERVER_IP = ip;
                SyncConfig.getConfig().SERVER_PORT = port;
                SyncConfig.getConfig().REFUSE_CLIENT_MODS = cbxRefuse.isSelected();

                try {
                    SyncConfig.getConfig().save();
                    updateLogsArea("Options saved");
                    btnSave.setDisable(true);
                } catch (IOException ex) {
                    Logger.debug(ex);
                    updateLogsArea(ex.toString());
                }
                btnSave.setDisable(false);
            }
        });

        setRowIndex(btnSave, 7);
        setColumnIndex(btnSave, 1);

        getChildren().addAll(btnCancel, btnSave);
        refreshConfigValues();
    }

    public TextField getAddressField() {
        if (address == null) {
            return new TextField();
        }
        return address;
    }

    public TextField getPortField() {
        if (port == null) {
            return new TextField();
        }
        return port;
    }

    public CheckBox getRefuseClientModsCheckbox() {
        if (refuseClientMods == null) {
            return new CheckBox();
        }
        return refuseClientMods;
    }

    public void refreshConfigValues() {
        getAddressField().setText(SyncConfig.getConfig().SERVER_IP);
        getPortField().setText(String.valueOf(SyncConfig.getConfig().SERVER_PORT));
        getRefuseClientModsCheckbox().setSelected(SyncConfig.getConfig().REFUSE_CLIENT_MODS);
    }

    public void updateLogsArea(String text) {
        Gui_JavaFX.getStackMainPane().getPaneLogs().updateLogsArea(text);
    }
}
