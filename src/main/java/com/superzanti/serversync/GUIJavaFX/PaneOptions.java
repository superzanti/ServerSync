package com.superzanti.serversync.GUIJavaFX;

import com.superzanti.serversync.config.ConfigLoader;
import com.superzanti.serversync.config.JsonConfig;
import com.superzanti.serversync.config.SyncConfig;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;

import java.io.IOException;

public class PaneOptions extends GridPane {

    public PaneOptions(){
        this.setPadding(new Insets(10,10,10,10));
        /** CLIENT */
        Label labelClient = new Label("Client (Options by default):");
        labelClient.setFont(new Font("Arial", 16));
        this.setRowIndex(labelClient, 0);
        this.setColumnIndex(labelClient, 0);

        Label labelAdress = new Label("address: ");
        this.setRowIndex(labelAdress, 1);
        this.setColumnIndex(labelAdress, 0);

        TextField fieldAdress = new TextField();
        fieldAdress.setText(SyncConfig.getConfig().SERVER_IP);
        this.setRowIndex(fieldAdress, 1);
        this.setColumnIndex(fieldAdress, 1);

        Label labelPort = new Label("port: ");
        this.setRowIndex(labelPort, 2);
        this.setColumnIndex(labelPort, 0);

        TextField fieldPort = new TextField();
        fieldPort.setText(String.valueOf(SyncConfig.getConfig().SERVER_PORT));
        this.setRowIndex(fieldPort, 2);
        this.setColumnIndex(fieldPort, 1);

        Label labelRefuse = new Label("refuse_client_mods: ");
        this.setRowIndex(labelRefuse, 3);
        this.setColumnIndex(labelRefuse, 0);

        CheckBox cbxRefuse = new CheckBox();
        if(SyncConfig.getConfig().REFUSE_CLIENT_MODS){
            cbxRefuse.setSelected(true);
        }else{
            cbxRefuse.setSelected(false);
        }
        this.setRowIndex(cbxRefuse, 3);
        this.setColumnIndex(cbxRefuse, 1);

        Label labelIgnore = new Label("Ignore list: ");
        this.setRowIndex(labelIgnore, 4);
        this.setColumnIndex(labelIgnore, 0);

        ListView<String> ignoreList = new ListView<String>();
        ObservableList<String> items = FXCollections.observableArrayList (SyncConfig.getConfig().FILE_IGNORE_LIST);
        ignoreList.setItems(items);
        this.setRowIndex(ignoreList, 5);
        this.setColumnIndex(ignoreList, 0);

        this.getChildren().addAll(labelClient, labelAdress, labelPort, labelRefuse, cbxRefuse, fieldAdress, fieldPort);
        this.getChildren().addAll(labelIgnore, ignoreList);


        /** SERVER */
        Label labelServer = new Label("Server (Options by  default):");
        this.setRowIndex(labelServer, 1);
        this.setColumnIndex(labelServer, 1);

        //this.getChildren().addAll(labelServer);

        /** SAVE BUTTON */
        Button btnSave = new Button("Save");
        btnSave.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                int port = Integer.parseInt(fieldPort.getText());
                String ip = fieldAdress.getText();
                if(Gui_JavaFX.getStackLoginPane().getPaneSync().setPort(port)){
                    Gui_JavaFX.getStackLoginPane().getPaneSync().setIPAddress(ip);

                    SyncConfig.getConfig().SERVER_IP = ip;
                    SyncConfig.getConfig().SERVER_PORT = port;
                    SyncConfig.getConfig().REFUSE_CLIENT_MODS = cbxRefuse.isSelected();

                    try {
                        JsonConfig.saveClient(ConfigLoader.v2ClientConfig);
                        Gui_JavaFX.getStackLoginPane().getPaneLogs().updateTextArea("Options saved");
                    } catch (IOException ioException) {
                        Gui_JavaFX.getStackLoginPane().getPaneLogs().updateTextArea(ioException.toString());
                    }
                }
            }
        });

        this.setRowIndex(btnSave, 6);
        this.setColumnIndex(btnSave, 1);

        this.getChildren().addAll(btnSave);
    }
}
