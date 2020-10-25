package com.superzanti.serversync.GUIJavaFX;

import com.superzanti.serversync.client.ClientWorker;
import com.superzanti.serversync.config.SyncConfig;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import javax.swing.*;
import javax.xml.soap.Text;

public class PaneMainMenu extends BorderPane {

    private SyncConfig config = SyncConfig.getConfig();

    private TableView table;
    private Button btnUpdate;
    private TextField fieldIp, fieldPort;
    private TextArea textArea;

    public PaneMainMenu(){
        Label label_filters = new Label("Filters          ");

        Label label_u = new Label("U: ");
        CheckBox cb_u = new CheckBox();
        Label label_i = new Label("I: ");
        CheckBox cb_i = new CheckBox();
        Label label_d = new Label("D: ");
        CheckBox cb_d = new CheckBox();

        HBox hbx_filters = new HBox();
        hbx_filters.setAlignment(Pos.BASELINE_RIGHT);
        hbx_filters.getChildren().addAll(label_filters, label_u, cb_u, label_i, cb_i, label_d, cb_d);
        this.setMargin(hbx_filters, new Insets(10, 10, 0, 0));
        this.setTop(hbx_filters);

        this.setMargin(getTableView(), new Insets(10, 10, 10, 10));
        //this.setCenter(getTableView());
        this.setCenter(getTextArea());
        GridPane gp = new GridPane();

        Label label_ip = new Label("IP:");
        gp.setRowIndex(label_ip, 0);
        gp.setColumnIndex(label_ip, 0);

        gp.setRowIndex(getFieldIp(), 1);
        gp.setColumnIndex(getFieldIp(), 0);

        Label label_port = new Label("Port:");
        gp.setRowIndex(label_port, 0);
        gp.setColumnIndex(label_port, 1);

        gp.setRowIndex(getFieldPort(), 1);
        gp.setColumnIndex(getFieldPort(), 1);

        gp.setRowIndex(getBtnUpdate(), 1);
        gp.setColumnIndex(getBtnUpdate(), 2);

        gp.getChildren().addAll(label_ip, label_port, getFieldIp(), getFieldPort(), getBtnUpdate());
        gp.setAlignment(Pos.CENTER);
        this.setMargin(gp, new Insets(0, 0, 10, 0));
        this.setBottom(gp);
    }

    public TableView getTableView(){
        if(table == null) {
            table = new TableView();
            table.setEditable(true);
            table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            //Create column
            TableColumn colFileName = new TableColumn("File name");
            TableColumn colOutdated = new TableColumn("Outdated");
            TableColumn colIgnored = new TableColumn("Ignored");

            colFileName.prefWidthProperty().bind(table.widthProperty().multiply(0.5));
            colOutdated.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
            colIgnored.prefWidthProperty().bind(table.widthProperty().multiply(0.2));

            table.getColumns().addAll(colFileName, colOutdated, colIgnored);

            //table.setItems(listQuestObser);

        }
        return table;
    }
    public Button getBtnUpdate(){
        if(btnUpdate == null){
            btnUpdate = new Button("Update");
            btnUpdate.setTooltip(new Tooltip("Synchronize client & server"));
            btnUpdate.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent e) {
                    int port = getPort();
                    String ip = getFieldIp().getText();
                    boolean error = false;
                    if (ip.equals("") || port == 90000) {
                        //updateText("No config found, requesting details");

                        if (ip.equals("")) {
                            ip = JOptionPane.showInputDialog("Server IP address");
                            setIPAddress(ip);
                        }

                        if (port == 90000) {
                            String serverPort = JOptionPane.showInputDialog("Server Port (numbers only)");
                            port = Integer.parseInt(serverPort);

                            if (setPort(port)) {
                                error = true;
                            }
                        }
                    }
                    if (!error) {
                        config.SERVER_IP = ip;
                        config.SERVER_PORT = port;
                        //TODO updateText("Starting update process...");
                        new Thread(new ClientWorker()).start();
                    }
                }
            });
        }
        return btnUpdate;
    }
    public TextField getFieldIp(){
        if(fieldIp == null){
            fieldIp = new TextField ();
            fieldIp.setPromptText("ex: 127.198.0.10");
        }
        return fieldIp;
    }
    public TextField getFieldPort(){
        if(fieldPort == null){
            fieldPort = new TextField ();
            fieldPort.setPromptText("ex: 25565");
        }
        return fieldPort;
    }
    public int getPort() {
        int port;
        try {
            port = Integer.parseInt(fieldPort.getText());
            if (!(port <= 49151 && port > 0)) {
                //updateText("Port out of range, valid range: 1 - 49151");
            }
        } catch (NumberFormatException e) {
            //updateText("Invalid port");
            port = 90000;
        }

        return port;
    }
    public boolean setPort(int port) {
        if (!(port <= 49151 && port > 0)) {
            updateText("Port out of range, valid range: 1 - 49151");
            return false;
        }

        getFieldPort().setText(String.valueOf(port));
        return true;
    }
    public void setIPAddress(String ip) {
        Platform.runLater(() -> fieldPort.setText(ip));
    }
    public void updateText(String text) {
        Platform.runLater(() -> textArea.setText(text));
    }
    public TextArea getTextArea(){
        if(textArea == null){
            textArea = new TextArea();
            textArea.setDisable(true);
        }
        return textArea;
    }
    /*public Button getBtnUpdate(){
        if(btnUpdate == null){

        }
        return btnUpdate;
    }*/
}

