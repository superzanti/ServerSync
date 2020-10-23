package com.superzanti.serversync.GUIJavaFX;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class PaneMainMenu extends BorderPane {

    TableView table;

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
        this.setTop(hbx_filters);

        this.setCenter(getTableView());

        GridPane gp = new GridPane();

        Label label_ip = new Label("IP:");
        gp.setRowIndex(label_ip, 0);
        gp.setColumnIndex(label_ip, 0);

        TextField field_ip = new TextField ();
        field_ip.setPromptText("ex: 127.198.0.10");
        gp.setRowIndex(field_ip, 1);
        gp.setColumnIndex(field_ip, 0);

        Label label_port = new Label("Port:");
        gp.setRowIndex(label_port, 0);
        gp.setColumnIndex(label_port, 1);

        TextField field_port = new TextField ();
        field_port.setPromptText("25565");
        gp.setRowIndex(field_port, 1);
        gp.setColumnIndex(field_port, 1);

        Button btn_download = new Button("Download");
        gp.setRowIndex(btn_download, 1);
        gp.setColumnIndex(btn_download, 2);

        gp.getChildren().addAll(label_ip, label_port, field_ip, field_port, btn_download);
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

}

