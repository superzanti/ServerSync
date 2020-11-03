package com.superzanti.serversync.GUIJavaFX;

import com.superzanti.serversync.client.ClientWorker;
import com.superzanti.serversync.config.Mod;
import com.superzanti.serversync.config.SyncConfig;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.control.Alert.AlertType;

public class PaneSync extends BorderPane {

    private SyncConfig config = SyncConfig.getConfig();

    private TableView table;
    private Button btnSync,btnCheckUpdate;
    private TextField fieldIp, fieldPort;
    private Alert alertWarning;
    private ObservableList<Mod> observMods = FXCollections.observableArrayList();
    private PaneProgressBar paneProgressBar;

    public PaneSync(){
        Label label_filters = new Label("Filters          ");

        Label label_u = new Label("U: ");
        CheckBox cb_u = new CheckBox();
        Label label_i = new Label("I: ");
        CheckBox cb_i = new CheckBox();
        Label label_d = new Label("D: ");
        CheckBox cb_d = new CheckBox();
        paneProgressBar = new PaneProgressBar();

        HBox hbx_filters = new HBox();
        hbx_filters.setAlignment(Pos.BASELINE_RIGHT);
        hbx_filters.getChildren().addAll(label_filters, label_u, cb_u, label_i, cb_i, label_d, cb_d);
        this.setMargin(hbx_filters, new Insets(10, 10, 0, 0));
        //this.setTop(hbx_filters);
        this.setTop(getPaneProgressBar());

        this.setMargin(getTableView(), new Insets(10, 10, 10, 10));
        this.setCenter(getTableView());

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

        gp.setRowIndex(getBtnSync(), 1);
        gp.setColumnIndex(getBtnSync(), 2);

        gp.setRowIndex(getBtnCheckUpdate(), 1);
        gp.setColumnIndex(getBtnCheckUpdate(), 3);

        gp.getChildren().addAll(label_ip, label_port, getFieldIp(), getFieldPort(), getBtnSync(),getBtnCheckUpdate());
        gp.setAlignment(Pos.CENTER);
        this.setMargin(gp, new Insets(0, 0, 10, 0));
        this.setBottom(gp);
    }

    public ObservableList<Mod> getObservMods(){
        return observMods;
    }

    public PaneProgressBar getPaneProgressBar(){
        if(paneProgressBar == null){
            paneProgressBar = new PaneProgressBar();
        }
        return paneProgressBar;
    }

    public TableView getTableView(){
        if(table == null) {

            table = new TableView();
            table.setEditable(true);
            table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            //Create column
            TableColumn<Mod, String> colFileName = new TableColumn<>("File name");
            TableColumn<Mod, String> colOutdated = new TableColumn("Outdated");
            TableColumn<Mod, Boolean> colIgnored = new TableColumn("Ignored");

            colFileName.prefWidthProperty().bind(table.widthProperty().multiply(0.5));
            colFileName.setCellValueFactory(
                    new PropertyValueFactory<>("name"));
            colOutdated.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
            colOutdated.setCellValueFactory(
                    new PropertyValueFactory<>("validValue"));
            colIgnored.prefWidthProperty().bind(table.widthProperty().multiply(0.2));
            colIgnored.setCellValueFactory(
                    new PropertyValueFactory<>("ignoreValue"));

            table.getColumns().addAll(colFileName, colOutdated, colIgnored);

            table.setItems(observMods);

            observMods.add(new Mod("zeub"));
        }

        return table;
    }
    public Boolean checkIpAndPort(String ip, int port){
        boolean valid = true;
        if (ip.equals("") && !setPort(port)) {
            updateLogsArea("No config found, requesting details");
            displayAlert("Bad config", "IP field is wrong \nPort out of range, valid range: 1 - 49151");
            valid = false;
        }
        else if (ip.equals("")) {
            updateLogsArea("The ip field is empty");
            displayAlert("Wrong IP", "The IP field is empty");
            valid = false;
        }else if(!setPort(port)) {
            updateLogsArea("The ip field is empty");
            displayAlert("Wrong port", "Port out of range, valid range: 1 - 49151");
            valid = false;
        }
        return valid;
    }
    public Button getBtnSync(){
        if(btnSync == null){
            btnSync = new Button("Sync");
            btnSync.getStyleClass().add("btn");
            btnSync.setTooltip(new Tooltip("Synchronize client & server"));
            btnSync.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent e) {
                    getBtnSync().setDisable(true);
                    getBtnCheckUpdate().setDisable(true);

                    int port = getPort();
                    String ip = getFieldIp().getText();
                    if (checkIpAndPort(ip, port)){
                        config.SERVER_IP = ip;
                        config.SERVER_PORT = port;
                        updateLogsArea("Starting update process...");
                        new Thread(new ClientWorker()).start();
                    }else{
                        getBtnSync().setDisable(false);
                        getBtnCheckUpdate().setDisable(false);
                    }
                }
            });
        }
        return btnSync;
    }
    public Button getBtnCheckUpdate(){
        if(btnCheckUpdate == null){
            btnCheckUpdate = new Button("Check for updates");
            btnCheckUpdate.getStyleClass().add("btn");
            btnCheckUpdate.getStyleClass().add("btnCheckUpdate");
            btnCheckUpdate.setTooltip(new Tooltip("Check update in table"));
            btnCheckUpdate.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent e) {
                    getBtnSync().setDisable(true);
                    getBtnCheckUpdate().setDisable(true);

                    int port = getPort();
                    String ip = getFieldIp().getText();
                    if (checkIpAndPort(ip, port)){
                        config.SERVER_IP = ip;
                        config.SERVER_PORT = port;
                        updateLogsArea("Starting update process...");
                        SyncConfig.getConfig().SYNC_MODE = 3;
                        new Thread(new ClientWorker()).start();
                    }else{
                        getBtnSync().setDisable(false);
                        getBtnCheckUpdate().setDisable(false);
                    }
                }
            });
        }
        return btnCheckUpdate;
    }
    public TextField getFieldIp(){
        if(fieldIp == null){
            fieldIp = new TextField ();
            fieldIp.setText(SyncConfig.getConfig().SERVER_IP);
        }
        return fieldIp;
    }
    public TextField getFieldPort(){
        if(fieldPort == null){
            fieldPort = new TextField ();
            fieldPort.setText(String.valueOf(SyncConfig.getConfig().SERVER_PORT));
        }
        return fieldPort;
    }
    public int getPort() {
        int port;
        try {
            port = Integer.parseInt(fieldPort.getText());
        } catch (NumberFormatException e) {
            updateLogsArea("Invalid port");
            port = -1;
        }

        return port;
    }
    public boolean setPort(int port) {
        if (!(port <= 49151 && port > 0)) {
            updateLogsArea("Port out of range, valid range: 1 - 49151");
            return false;
        }
        Platform.runLater(() -> fieldPort.setText(String.valueOf(port)));
        return true;
    }
    public void setIPAddress(String ip) {
        Platform.runLater(() -> fieldIp.setText(ip));
    }
    public void updateLogsArea(String text) {
        Gui_JavaFX.getStackMainPane().getPaneLogs().updateLogsArea(text);
    }
    public void displayAlert(String header, String content){
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Warning Dialog");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

