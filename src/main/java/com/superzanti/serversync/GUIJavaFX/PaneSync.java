package com.superzanti.serversync.GUIJavaFX;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.client.ActionEntry;
import com.superzanti.serversync.client.ClientWorker;
import com.superzanti.serversync.client.EActionType;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.Then;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Callable;

// GUI of the SYNC panel (Field ip/port, button "sync" and "check for updates", table with mods)
public class PaneSync extends BorderPane {

    private final SyncConfig config = SyncConfig.getConfig();

    private TableView<ActionEntry> table;
    private Button btnSync, btnCheckUpdate;
    private TextField fieldIp, fieldPort;
    private final ObservableList<ActionEntry> observableMods = FXCollections.observableArrayList();
    private PaneProgressBar paneProgressBar;

    private final ClientWorker worker = new ClientWorker();

    public PaneSync() {
        /*TODO Filter for the table */
        /*
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
        */

        /* Progress bar */
        this.setTop(getPaneProgressBar());

        /* Table with mods */
        setMargin(getTableView(), new Insets(10, 10, 10, 10));
        this.setCenter(getTableView());

        /* Bottom section with IP/Port field and label, button "Sync" and "Check for updates */
        GridPane gp = new GridPane();

        Label label_ip = I18N.labelForValue(() -> I18N.get("ui/server_address"));
        GridPane.setRowIndex(label_ip, 0);
        GridPane.setColumnIndex(label_ip, 0);

        GridPane.setRowIndex(getFieldIp(), 1);
        GridPane.setColumnIndex(getFieldIp(), 0);

        Label label_port = I18N.labelForValue(() -> I18N.get("ui/server_port"));
        GridPane.setRowIndex(label_port, 0);
        GridPane.setColumnIndex(label_port, 1);

        GridPane.setRowIndex(getFieldPort(), 1);
        GridPane.setColumnIndex(getFieldPort(), 1);

        GridPane.setRowIndex(getBtnSync(), 1);
        GridPane.setColumnIndex(getBtnSync(), 2);

        GridPane.setRowIndex(getBtnCheckUpdate(), 1);
        GridPane.setColumnIndex(getBtnCheckUpdate(), 3);

        gp.getChildren().addAll(label_ip, label_port, getFieldIp(), getFieldPort(), getBtnSync(), getBtnCheckUpdate());
        gp.setAlignment(Pos.CENTER);
        setMargin(gp, new Insets(0, 0, 10, 0));
        this.setBottom(gp);
    }

    public ObservableList<ActionEntry> getObservableMods() {
        return observableMods;
    }

    public PaneProgressBar getPaneProgressBar() {
        if (paneProgressBar == null) {
            paneProgressBar = new PaneProgressBar();
        }
        return paneProgressBar;
    }

    public TableView<ActionEntry> getTableView() {
        if (table == null) {

            table = new TableView<>();
            table.setEditable(true);
            table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            // Create columns
            TableColumn<ActionEntry, String> colFileName = I18N.tableColumnForKey("ui/file_path");
            colFileName.prefWidthProperty().bind(table.widthProperty().multiply(0.55));
            colFileName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colFileName.getStyleClass().add("align-left");
            //----

            TableColumn<ActionEntry, EActionType> colStatus = I18N.tableColumnEActionForKey("ui/action");
            colStatus.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
            colStatus.setCellValueFactory(new PropertyValueFactory<>("action"));
            colStatus.setCellFactory(tc -> new TableCell<ActionEntry, EActionType>() {

                @Override
                protected void updateItem(EActionType item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null) {
                        setStyle("");
                        return;
                    }
                    try {
                        setText(I18N.get("ui/action_" + item.toString()));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    switch (item) {
                        case Ignore:
                            setStyle("-fx-text-fill: #db5461;");
                            break;
                        case Update:
                            setStyle("-fx-text-fill: #dfa06e;");
                            break;
                        case None:
                            setStyle("-fx-text-fill: #86ba90;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            });
            //----

            // TODO ignore not implemented yet
//            TableColumn<ActionEntry, Boolean> colIgnored = new TableColumn<>("Ignored");
//            colIgnored.prefWidthProperty().bind(table.widthProperty().multiply(0.2));
//            colIgnored.setCellValueFactory(new PropertyValueFactory<>("action"));
            //----

            TableColumn<ActionEntry, String> colReason = I18N.tableColumnForKey("ui/reason");
            colReason.prefWidthProperty().bind(table.widthProperty().multiply(0.25));
            colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
            colReason.setCellFactory(tc -> new TableCell<ActionEntry, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        try {
                            setText(I18N.get(item));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            table.getColumns().addAll(colStatus, colFileName, colReason);

            table.setItems(observableMods);
        }

        return table;
    }

    public Boolean checkIpAndPort(String ip, int port) {
        boolean valid = true;
        if (ip.equals("") && !setPort(port)) {
            updateLogsArea("No config found, requesting details");
            displayAlert(
                ServerSync.strings.getString("ui/wrong_configuration"),
                ServerSync.strings.getString("ui/ip_empty") + "\n" + ServerSync.strings.getString("ui/port_invalid")
            );
            valid = false;
        } else if (ip.equals("")) {
            updateLogsArea("The ip field is empty");
            displayAlert(ServerSync.strings.getString("ui/wrong_ip"), ServerSync.strings.getString("ui/ip_empty"));
            valid = false;
        } else if (!setPort(port)) {
            updateLogsArea("The ip field is empty");
            displayAlert(
                ServerSync.strings.getString("ui/wrong_port"), ServerSync.strings.getString("ui/port_invalid"));
            valid = false;
        }
        return valid;
    }

    public Button getBtnSync() {
        if (btnSync == null) {
            btnSync = I18N.buttonForKey(("ui/sync"));
            btnSync.getStyleClass().add("btn");
            btnSync.setTooltip(I18N.toolTipForKey("ui/btn_sync_tooltip"));
            btnSync.setOnAction(e -> {
                Logger.debug("Clicked sync button");
                getBtnSync().setDisable(true);
                getBtnCheckUpdate().setDisable(true);

                int port = getPort();
                String ip = getFieldIp().getText();
                if (checkIpAndPort(ip, port)) {
                    ObservableList<ActionEntry> list = Gui_JavaFX
                        .getStackMainPane()
                        .getPaneSync()
                        .getObservableMods();
                    updateLogsArea("Starting update process...");

                    try {
                        worker.setAddress(ip);
                        worker.setPort(port);
                        worker.connect();

                        // Setting this after connection so that we don't save an invalid address
                        SyncConfig.getConfig().SERVER_IP = ip;
                        SyncConfig.getConfig().SERVER_PORT = port;
                        saveConfig();

                        setProgressText(ServerSync.strings.getString("connection_attempt_server"));
                        Then.onComplete(worker.fetchActions(), actions -> {
                            list.clear();
                            list.addAll(actions);

                            Callable<Void> sync = worker
                                .executeActions(actions, actionProgress -> Platform.runLater(() -> {
                                    getPaneProgressBar().setPathText(actionProgress.getName());
                                    getPaneProgressBar().getProgressBar().setProgress(actionProgress.getProgress());
                                    if (actionProgress.isComplete()) {
                                        getObservableMods()
                                            .stream().filter(entry -> entry.equals(actionProgress.entry))
                                            .findAny()
                                            .ifPresent(v -> {
                                                v.action = EActionType.None;
                                                v.reason = "ui/reason_updated";
                                            });
                                    }
                                    getTableView().refresh();
                                    getPaneProgressBar().updateGUI();
                                }));
                            Then.onComplete(sync, unused -> Platform.runLater(() -> {
                                setProgressText(ServerSync.strings.getString("update_complete"));
                                worker.close();
                            }));
                        });
                    } catch (Exception exception) {
                        Logger.debug(exception);
                        setProgressText(
                            ServerSync.strings.getString("connection_failed_server") + " " + ip + ":" + port);
                    }
                }

                getBtnSync().setDisable(false);
                getBtnCheckUpdate().setDisable(false);
            });
        }
        return btnSync;
    }

    public Button getBtnCheckUpdate() {
        if (btnCheckUpdate == null) {
            btnCheckUpdate = I18N.buttonForKey(("ui/check_for_updates"));
            btnCheckUpdate.getStyleClass().add("btn");
            btnCheckUpdate.setTooltip(I18N.toolTipForKey("ui/btn_check_tooltip"));
            btnCheckUpdate.setOnAction(e -> {
                Logger.debug("Clicked check updates button");
                getBtnSync().setDisable(true);
                getBtnCheckUpdate().setDisable(true);

                int port = getPort();
                String ip = getFieldIp().getText();
                if (checkIpAndPort(ip, port)) {
                    ObservableList<ActionEntry> list = Gui_JavaFX
                        .getStackMainPane()
                        .getPaneSync()
                        .getObservableMods();
                    updateLogsArea("Starting update process...");
                    //setProgressText("Fetching manifest...");
                    setProgressText(ServerSync.strings.getString("connection_attempt_server"));
                    list.clear();
                    worker.setAddress(ip);
                    worker.setPort(port);

                    try {
                        worker.connect();

                        // Setting this after connection so that we don't save an invalid address
                        SyncConfig.getConfig().SERVER_IP = ip;
                        SyncConfig.getConfig().SERVER_PORT = port;
                        saveConfig();

                        Then.onComplete(worker.fetchActions(), actions -> {
                            list.addAll(actions);
                            worker.close();
                            Platform.runLater(() -> setProgressText(""));
                        });
                    } catch (Exception exception) {
                        Logger.debug(exception);
                        setProgressText(
                            ServerSync.strings.getString("connection_failed_server") + " " + ip + ":" + port);
                    }
                }

                getBtnSync().setDisable(false);
                getBtnCheckUpdate().setDisable(false);
            });
        }
        return btnCheckUpdate;
    }

    public TextField getFieldIp() {
        if (fieldIp == null) {
            fieldIp = new TextField();
            fieldIp.setText(SyncConfig.getConfig().SERVER_IP);
        }
        return fieldIp;
    }

    public TextField getFieldPort() {
        if (fieldPort == null) {
            fieldPort = new TextField();
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

    public void setProgressText(String text) {
        getPaneProgressBar().setPathText(text);
        getPaneProgressBar().updateGUI();
    }

    public void setProgress(double progress) {
        getPaneProgressBar().getProgressBar().setProgress(progress);
    }

    public boolean setPort(int port) {
        if (port > 49151 || port < 0) {
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

    public void displayAlert(String header, String content) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Warning Dialog");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void saveConfig() {
        try {
            SyncConfig.getConfig().save();
            updateLogsArea("Options saved");
        } catch (IOException ex) {
            Logger.debug(ex);
            updateLogsArea(ex.toString());
        }
    }
}

